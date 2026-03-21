package com.bamboo.postService.dto.count;

import com.bamboo.postService.common.enums.Visibility;

import java.util.UUID;

public record ContentCountEvent(
        UUID userId,
        String contentType,
        String action,
        Visibility oldVisibility,
        Visibility newVisibility) {}
