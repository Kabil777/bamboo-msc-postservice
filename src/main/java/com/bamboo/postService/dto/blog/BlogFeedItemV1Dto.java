package com.bamboo.postService.dto.blog;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BlogFeedItemV1Dto(
        UUID id,
        String title,
        String coverUrl,
        String description,
        List<String> tags,
        Instant createdAt,
        Visibility visibility,
        PostStatus status,
        AuthorSummaryV1Dto author,
        List<AuthorSummaryV1Dto> collaborators) {}
