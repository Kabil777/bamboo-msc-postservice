package com.bamboo.postService.common.response;

import java.time.Instant;

public record CommonResponse<T>(int status, T data, Instant timestamp) {}
