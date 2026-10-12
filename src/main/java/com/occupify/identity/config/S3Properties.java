package com.occupify.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {

    /**
     * Oracle Cloud Infrastructure (OCI) Object Storage namespace (optional for standard AWS S3).
     */
    private String bucketNamespace;

    /**
     * Cloud Region (e.g., ap-singapore-1, us-east-1).
     */
    private String region = "ap-singapore-1";

    /**
     * Custom endpoint URL for OCI S3-compat, MinIO, or Cloudflare R2 (optional for standard AWS S3).
     * Example for OCI: https://{namespace}.compat.objectstorage.{region}.oraclecloud.com
     */
    private String endpoint;

    /**
     * Object Storage Bucket Name.
     */
    private String bucketName = "occupify-storage";

    /**
     * Access Key ID / Customer Secret Key Access ID.
     */
    private String accessKey;

    /**
     * Secret Access Key / Customer Secret Key.
     */
    private String secretKey;

    /**
     * Custom Public URL prefix (CDN or OCI public bucket URL).
     * Example: https://objectstorage.ap-singapore-1.oraclecloud.com/n/{namespace}/b/{bucketName}/o
     */
    private String publicUrlPrefix;

    /**
     * Whether to use path-style access (mandatory for OCI S3-compat & MinIO).
     */
    private boolean pathStyleAccessEnabled = true;
}
