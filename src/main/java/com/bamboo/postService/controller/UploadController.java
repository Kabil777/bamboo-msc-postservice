package com.bamboo.postService.controller;

import com.bamboo.postService.dto.upload.PlainImageUploadResponse;
import com.bamboo.postService.service.BucketUploadService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final BucketUploadService bucketUploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlainImageUploadResponse> upload(@RequestPart("file") MultipartFile file)
            throws IOException {
        return bucketUploadService.uploadPlainImage(file);
    }

    @PostMapping(value = "/image/url")
    public ResponseEntity<PlainImageUploadResponse> uploadFromUrl(@RequestParam("url") String url)
            throws IOException, InterruptedException {
        return bucketUploadService.uploadImageFromUrl(url);
    }
}
