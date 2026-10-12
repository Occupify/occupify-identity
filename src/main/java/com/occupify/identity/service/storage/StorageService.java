package com.occupify.identity.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Upload an image (avatar, attachment) to object storage.
     *
     * @param directory Target directory/prefix in the bucket (e.g. "avatars/{userId}")
     * @param file      Uploaded multipart file
     * @return Public URL to the uploaded object
     */
    String uploadImage(String directory, MultipartFile file);

    /**
     * Delete an object from object storage given its public URL or object key.
     *
     * @param fileUrlOrKey Full public URL or object key
     */
    void deleteFile(String fileUrlOrKey);
}
