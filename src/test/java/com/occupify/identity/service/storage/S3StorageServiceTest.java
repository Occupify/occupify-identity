package com.occupify.identity.service.storage;

import com.occupify.identity.config.S3Properties;
import com.occupify.identity.exception.storage.StorageErrorCode;
import com.occupify.identity.exception.storage.StorageException;
import com.occupify.identity.service.storage.impl.S3StorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3StorageService Unit Tests (QA Test Suite)")
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Properties properties;
    private S3StorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        properties = new S3Properties();
        properties.setBucketName("occupify-test-bucket");
        properties.setBucketNamespace("axoxlxowxohg");
        properties.setRegion("ap-singapore-1");
        properties.setEndpoint("https://axoxlxowxohg.compat.objectstorage.ap-singapore-1.oraclecloud.com");

        storageService = new S3StorageServiceImpl(s3Client, properties);
    }

    @Test
    @DisplayName("uploadImage -> when valid JPEG file -> should upload to S3 and return OCI public URL")
    void uploadImage_whenValidJpeg_shouldUploadSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my-avatar.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String resultUrl = storageService.uploadImage("avatars/user-123", file);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("occupify-test-bucket");
        assertThat(capturedRequest.key()).startsWith("avatars/user-123/");
        assertThat(capturedRequest.key()).endsWith(".jpg");
        assertThat(capturedRequest.contentType()).isEqualTo("image/jpeg");

        assertThat(resultUrl)
                .startsWith("https://objectstorage.ap-singapore-1.oraclecloud.com/n/axoxlxowxohg/b/occupify-test-bucket/o/avatars/user-123/");
    }

    @Test
    @DisplayName("uploadImage -> when file is empty -> should throw StorageException FILE_EMPTY")
    void uploadImage_whenEmptyFile_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> storageService.uploadImage("avatars/user-123", emptyFile))
                .isInstanceOf(StorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", StorageErrorCode.FILE_EMPTY);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage -> when file exceeds 5MB limit -> should throw StorageException FILE_TOO_LARGE")
    void uploadImage_whenFileExceedsLimit_shouldThrowException() {
        byte[] oversizedBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                oversizedBytes
        );

        assertThatThrownBy(() -> storageService.uploadImage("avatars/user-123", largeFile))
                .isInstanceOf(StorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", StorageErrorCode.FILE_TOO_LARGE);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage -> when file has disallowed MIME type (e.g. PDF) -> should throw StorageException INVALID_FILE_TYPE")
    void uploadImage_whenInvalidContentType_shouldThrowException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        assertThatThrownBy(() -> storageService.uploadImage("avatars/user-123", pdfFile))
                .isInstanceOf(StorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", StorageErrorCode.INVALID_FILE_TYPE);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage -> when S3Client throws S3Exception -> should wrap in StorageException UPLOAD_FAILED")
    void uploadImage_whenS3ClientFails_shouldThrowUploadFailed() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "valid bytes".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied").build());

        assertThatThrownBy(() -> storageService.uploadImage("avatars/user-123", file))
                .isInstanceOf(StorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", StorageErrorCode.UPLOAD_FAILED);
    }

    @Test
    @DisplayName("deleteFile -> when valid OCI URL provided -> should extract key and call S3Client deleteObject")
    void deleteFile_whenValidOciUrl_shouldDeleteFromS3() {
        String ociUrl = "https://objectstorage.ap-singapore-1.oraclecloud.com/n/axoxlxowxohg/b/occupify-test-bucket/o/avatars/user-123/uuid-abc.png";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        storageService.deleteFile(ociUrl);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("occupify-test-bucket");
        assertThat(request.key()).isEqualTo("avatars/user-123/uuid-abc.png");
    }

    @Test
    @DisplayName("deleteFile -> when external 3rd-party URL (e.g. Unsplash) provided -> should skip S3 deletion")
    void deleteFile_whenExternalUrl_shouldSkipDeletion() {
        String externalUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500";

        storageService.deleteFile(externalUrl);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("deleteFile -> when URL is null or empty -> should safely do nothing")
    void deleteFile_whenNullOrEmpty_shouldDoNothing() {
        storageService.deleteFile(null);
        storageService.deleteFile("");
        storageService.deleteFile("   ");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("deleteFile -> when S3Client fails -> should throw StorageException DELETE_FAILED")
    void deleteFile_whenS3ClientFails_shouldThrowDeleteFailed() {
        String key = "avatars/user-123/uuid-abc.png";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("Bucket unavailable").build());

        assertThatThrownBy(() -> storageService.deleteFile(key))
                .isInstanceOf(StorageException.class)
                .hasFieldOrPropertyWithValue("errorCode", StorageErrorCode.DELETE_FAILED);
    }
}
