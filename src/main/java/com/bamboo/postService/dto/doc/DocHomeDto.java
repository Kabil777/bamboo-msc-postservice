package com.bamboo.postService.dto.doc;

import java.time.Instant;
import java.util.UUID;
import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;

public record DocHomeDto(
        UUID id,
        String title,
        String coverUrl,
        String description,
        Instant createdAt,
        Visibility visibility,
        PostStatus status) {}
