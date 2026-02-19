package com.bamboo.postService.service;

import com.bamboo.postService.common.response.CommonResponse;

import org.springframework.http.ResponseEntity;

public interface CommonServices {
    public <T> ResponseEntity<CommonResponse<String>> save(T blogPostDto, String userId);
}
