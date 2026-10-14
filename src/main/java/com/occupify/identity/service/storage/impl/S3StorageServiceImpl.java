package com.occupify.identity.service.storage.impl;

import com.occupify.identity.config.S3Properties;
import com.occupify.identity.constant.StorageConstants;
import com.occupify.identity.exception.storage.StorageErrorCode;
import com.occupify.identity.exception.storage.StorageException;
import com.occupify.identity.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements StorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif");

    private final S3Client s3Client;
    private final S3Properties properties;

    /**
     * Tải lên một file ảnh công khai (Public Asset) lên Object Storage (AWS S3 /
     * OCI Object Storage)
     * và trả về đường dẫn tĩnh công khai (Public URL).
     * <p>
     * <b>Mục đích sử dụng:</b><br>
     * Dùng cho các file công khai như Avatar người dùng, Banner, Logo cần hiển thị
     * trực tiếp
     * trên trình duyệt web/mobile mà không cần xác thực chữ ký (Bucket phải ở chế
     * độ Public Read).
     * </p>
     * <p>
     * <b>Lưu ý kiến trúc (Public vs Private):</b>
     * <ul>
     * <li>Hàm này <b>KHÔNG</b> dùng cho các file nhạy cảm/bảo mật (như Hợp đồng,
     * CCCD, bảng lương).</li>
     * <li>Với các file private, trong tương lai sẽ bổ sung phương thức riêng sử
     * dụng cơ chế
     * <i>Presigned URL</i> (URL có thời hạn tạm thời) kết hợp với Bucket ở chế độ
     * Private.</li>
     * </ul>
     * </p>
     *
     * @param directory Thư mục mục tiêu trong Bucket (ví dụ: "avatars/{userId}")
     * @param file      Tệp tin multipart tải lên từ client (tối đa 5MB, định dạng:
     *                  JPEG, PNG, WEBP, GIF)
     * @return Đường dẫn URL công khai tĩnh (Public URL) trỏ trực tiếp đến file trên
     *         Object Storage
     * @throws StorageException nếu file rỗng, vượt quá 5MB, sai định dạng MIME
     *                          type, hoặc upload thất bại
     */
    @Override
    public String uploadImage(String directory, MultipartFile file) {
        validateImageFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename, file.getContentType());
        String cleanDirectory = directory != null ? directory.replaceAll("^/+|/+$", "") : "uploads";
        String key = String.format("%s/%s%s", cleanDirectory, UUID.randomUUID(), extension);

        log.info("Uploading image to S3: bucket={}, key={}, size={} bytes, contentType={}",
                properties.getBucketName(), key, file.getSize(), file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            String publicUrl = constructPublicUrl(key);
            log.info("Successfully uploaded image. Public URL: {}", publicUrl);
            return publicUrl;
        } catch (IOException e) {
            log.error("Failed to read input stream for file upload: {}", originalFilename, e);
            throw new StorageException(StorageErrorCode.UPLOAD_FAILED, "Failed to read uploaded file", e);
        } catch (Exception e) {
            log.error("Failed to upload object to S3: bucket={}, key={}", properties.getBucketName(), key, e);
            throw new StorageException(StorageErrorCode.UPLOAD_FAILED, "S3 upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrlOrKey) {
        if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) {
            return;
        }

        String key = extractObjectKey(fileUrlOrKey);
        if (key == null || key.isBlank()) {
            log.info("Skipping delete for non-S3/external URL: {}", fileUrlOrKey);
            return;
        }

        log.info("Deleting object from S3: bucket={}, key={}", properties.getBucketName(), key);
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted object from S3: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete object from S3: bucket={}, key={}", properties.getBucketName(), key, e);
            throw new StorageException(StorageErrorCode.DELETE_FAILED, "S3 delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra tính hợp lệ của file tải lên (không rỗng, dung lượng tối đa 5MB,
     * đúng định dạng ảnh).
     *
     * @param file File multipart gửi lên từ client
     * @throws StorageException nếu file rỗng, vượt quá 5MB hoặc sai định dạng MIME
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException(StorageErrorCode.FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageException(StorageErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new StorageException(StorageErrorCode.INVALID_FILE_TYPE);
        }
    }

    /**
     * Trích xuất đuôi mở rộng của file dựa trên tên gốc hoặc Content-Type.
     *
     * @param filename    Tên gốc của file
     * @param contentType Chuỗi MIME type của file
     * @return Đuôi mở rộng dạng chuỗi (ví dụ: .png, .jpg, .webp)
     */
    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            if (ext.matches("^\\.[a-z0-9]+$")) {
                return ext;
            }
        }
        if ("image/png".equalsIgnoreCase(contentType))
            return ".png";
        if ("image/webp".equalsIgnoreCase(contentType))
            return ".webp";
        if ("image/gif".equalsIgnoreCase(contentType))
            return ".gif";
        return ".jpg";
    }

    /**
     * Xây dựng URL công khai của đối tượng đã upload trên cloud.
     * Thứ tự ưu tiên: Custom CDN/Prefix -> Oracle Cloud (OCI) Native URL -> Custom
     * S3 Endpoint -> AWS S3.
     *
     * @param key Đường dẫn định danh của đối tượng trong bucket (object key)
     * @return Đường dẫn URL đầy đủ để client có thể truy cập xem ảnh
     */
    private String constructPublicUrl(String key) {
        if (properties.getPublicUrlPrefix() != null && !properties.getPublicUrlPrefix().isBlank()) {
            return String.format("%s/%s", stripTrailingSlash(properties.getPublicUrlPrefix().trim()), key);
        }

        if (properties.getBucketNamespace() != null && !properties.getBucketNamespace().isBlank()) {
            String region = properties.getRegion() != null ? properties.getRegion().trim() : StorageConstants.DEFAULT_OCI_REGION;
            return String.format(StorageConstants.OCI_PUBLIC_URL_FORMAT,
                    region, properties.getBucketNamespace().trim(), properties.getBucketName().trim(), key);
        }

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            return String.format("%s/%s/%s", stripTrailingSlash(properties.getEndpoint().trim()),
                    properties.getBucketName().trim(), key);
        }

        String region = properties.getRegion() != null ? properties.getRegion().trim() : StorageConstants.DEFAULT_AWS_REGION;
        return String.format(StorageConstants.AWS_PUBLIC_URL_FORMAT, properties.getBucketName().trim(), region, key);
    }

    /**
     * Phân tích và trích xuất Object Key từ URL công khai hoặc chuỗi key thô.
     * Nếu là URL bên ngoài không thuộc bucket của hệ thống (ví dụ ảnh Unsplash),
     * trả về null để bỏ qua xóa.
     *
     * @param fileUrlOrKey URL đầy đủ hoặc Object Key cần phân tích
     * @return Object Key thực tế trong bucket, hoặc null nếu là URL bên ngoài
     */
    @Nullable
    private String extractObjectKey(String fileUrlOrKey) {
        if (!fileUrlOrKey.startsWith("http://") && !fileUrlOrKey.startsWith("https://")) {
            return fileUrlOrKey.replaceAll("^/+", "");
        }

        if (fileUrlOrKey.contains("/o/")) {
            return fileUrlOrKey.substring(fileUrlOrKey.indexOf("/o/") + 3);
        }

        String bucketMarker = "/" + properties.getBucketName() + "/";
        if (fileUrlOrKey.contains(bucketMarker)) {
            return fileUrlOrKey.substring(fileUrlOrKey.indexOf(bucketMarker) + bucketMarker.length());
        }

        if (properties.getPublicUrlPrefix() != null && !properties.getPublicUrlPrefix().isBlank()) {
            String prefix = stripTrailingSlash(properties.getPublicUrlPrefix().trim());
            if (fileUrlOrKey.startsWith(prefix)) {
                return fileUrlOrKey.substring(prefix.length()).replaceAll("^/+", "");
            }
        }

        return null;
    }

    /**
     * Loại bỏ các ký tự dấu gạch chéo dư thừa ở cuối chuỗi URL hoặc tiền tố.
     *
     * @param str Chuỗi cần làm sạch
     * @return Chuỗi đã loại bỏ dấu gạch chéo ở cuối
     */
    private String stripTrailingSlash(String str) {
        return str != null ? str.replaceAll("/+$", "") : "";
    }
}
