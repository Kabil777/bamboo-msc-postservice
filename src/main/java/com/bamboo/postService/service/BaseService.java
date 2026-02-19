package com.bamboo.postService.service;

import com.bamboo.postService.common.response.CommonResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

abstract class BaseService {
    protected <T> ResponseEntity<CommonResponse<T>> buildResponse(HttpStatus status, T body) {

        CommonResponse<T> response = new CommonResponse<>(status.value(), body, Instant.now());

        return ResponseEntity.status(status).body(response);
    }
}
