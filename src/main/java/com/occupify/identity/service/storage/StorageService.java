package com.occupify.identity.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Tải lên một file ảnh công khai (Public Asset) lên Object Storage và trả về đường dẫn tĩnh công khai (Public URL).
     * <p>
     * <b>Mục đích sử dụng:</b><br>
     * Hàm này chuyên phục vụ cho các tài nguyên công khai như ảnh đại diện người dùng (Avatar),
     * logo công ty, ảnh bìa, hoặc các tệp tin media cần hiển thị tự do trên giao diện web/mobile
     * mà không yêu cầu quyền hạn hay chữ ký xác thực người xem. Yêu cầu Bucket trên Cloud Storage
     * phải được cấu hình ở chế độ <b>Public Read</b>.
     * </p>
     * <p>
     * <b>Ghi chú kiến trúc (Public vs Private Storage):</b>
     * <ul>
     *   <li>Hàm hiện tại chỉ áp dụng mô hình <i>Direct Public URL</i>, cho phép trình duyệt và CDN
     *   cache ảnh tự do nhằm tối ưu hóa tốc độ tải trang và giảm tải tính toán cho backend.</li>
     *   <li>Đối với các tài liệu nhạy cảm / riêng tư (Private Assets như hợp đồng kinh tế, CCCD/CMND, bảng lương),
     *   <b>KHÔNG</b> sử dụng hàm này. Khi có nhu cầu lưu trữ tài liệu riêng tư, hệ thống sẽ mở rộng
     *   thêm phương thức riêng sử dụng cơ chế <i>Presigned URL</i> (URL có chữ ký số tạm thời kèm thời gian hết hạn)
     *   kết hợp với Bucket ở chế độ Private.</li>
     * </ul>
     * </p>
     *
     * @param directory Thư mục mục tiêu trong Bucket (ví dụ: "avatars/{userId}")
     * @param file      Tệp tin multipart tải lên từ client (tối đa 5MB, định dạng: JPEG, PNG, WEBP, GIF)
     * @return Đường dẫn URL công khai tĩnh (Public URL) trỏ trực tiếp đến file trên Object Storage
     * @throws com.occupify.identity.exception.storage.StorageException nếu file rỗng, vượt quá 5MB,
     *         sai định dạng MIME type, hoặc quá trình upload lên cloud thất bại
     */
    String uploadImage(String directory, MultipartFile file);

    /**
     * Xóa một đối tượng khỏi Object Storage dựa trên URL công khai hoặc Object Key.
     *
     * @param fileUrlOrKey URL đầy đủ hoặc Object Key của đối tượng cần xóa
     */
    void deleteFile(String fileUrlOrKey);
}
