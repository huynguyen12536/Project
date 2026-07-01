package com.learnhub.upload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class StorageClientConfig {

    @Bean
    public S3Client multipartS3Client(StorageProperties storageProperties) {
        return S3Client.builder()
            .endpointOverride(URI.create(storageProperties.getEndpoint()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                )
            )
            .region(Region.of(storageProperties.getRegion()))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
    }

    @Bean
    public S3Presigner multipartS3Presigner(StorageProperties storageProperties) {
        return S3Presigner.builder()
            .endpointOverride(URI.create(storageProperties.getPublicEndpoint()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                )
            )
            .region(Region.of(storageProperties.getRegion()))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build()
            )
            .build();
    }
}
