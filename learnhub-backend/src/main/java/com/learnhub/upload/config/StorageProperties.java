package com.learnhub.upload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class StorageProperties {

    @Value("${minio.endpoint:http://minio:9000}")
    private String endpoint;

    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${minio.bucket-name:avatars}")
    private String bucketName;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;
}
