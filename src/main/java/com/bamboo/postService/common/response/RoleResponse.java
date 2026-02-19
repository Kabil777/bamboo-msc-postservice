package com.bamboo.postService.common.response;

import org.springframework.http.HttpStatus;

public record RoleResponse(
        boolean ok, String role, boolean readOnly, HttpStatus code, String message) {}
