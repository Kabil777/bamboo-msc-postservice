package com.bamboo.postService.service;

import com.bamboo.postService.dto.upload.PlainImageUploadResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BucketUploadService {

    private final S3Client s3Client;

    @Value("${s3.bucket.baseUrl}")
    private String uploadBaseUrl;

    @Value("${s3.bucket.name}")
    private String bucket;

    public ResponseEntity<PlainImageUploadResponse> uploadPlainImage(MultipartFile image)
            throws IOException {
        String key = "images/" + UUID.randomUUID() + "-" + image.getOriginalFilename();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(image.getContentType())
                        .build(),
                RequestBody.fromBytes(image.getBytes()));

        return ResponseEntity.ok(
                new PlainImageUploadResponse(true, uploadBaseUrl + "/" + bucket + "/" + key));
    }

    public ResponseEntity<PlainImageUploadResponse> uploadImageFromUrl(String url)
            throws IOException, InterruptedException {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http/https URLs are supported");
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to download image. Status: " + response.statusCode());
        }

        Optional<String> contentTypeOpt =
                response.headers().firstValue("Content-Type");
        String contentType = contentTypeOpt.orElse("application/octet-stream");

        String fileName = Optional.ofNullable(uri.getPath())
                .filter(p -> !p.isBlank())
                .map(p -> p.substring(p.lastIndexOf('/') + 1))
                .filter(p -> !p.isBlank())
                .orElse("remote-file");
        String key = "images/" + UUID.randomUUID() + "-" + fileName;

        Optional<String> contentLengthOpt =
                response.headers().firstValue("Content-Length");
        if (contentLengthOpt.isPresent()) {
            long contentLength = Long.parseLong(contentLengthOpt.get());
            try (InputStream body = response.body()) {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(contentType)
                                .contentLength(contentLength)
                                .build(),
                        RequestBody.fromInputStream(body, contentLength));
            }
        } else {
            byte[] bytes = response.body().readAllBytes();
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes));
        }

        return ResponseEntity.ok(
                new PlainImageUploadResponse(true, uploadBaseUrl + "/" + bucket + "/" + key));
    }
}
