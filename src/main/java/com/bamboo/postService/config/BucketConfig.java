package com.bamboo.postService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Configuration
public class BucketConfig {

    @Value("${s3.bucket.name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        S3Client s3 =
                S3Client.builder()
                        .endpointOverride(URI.create("http://localhost:9000"))
                        .region(Region.US_EAST_1)
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create("rustfsadmin", "kabil777")))
                        .forcePathStyle(true)
                        .build();

        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            } else {
                throw e;
            }
        }

        return s3;
    }
}
