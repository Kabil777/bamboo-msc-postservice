package com.bamboo.postService.dto.doc;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.dto.common.AuthorSummaryV1Dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record DocDetailV1Dto(
        UUID id,
        String title,
        String coverUrl,
        String description,
        String content,
        Set<String> tags,
        Instant createdAt,
        Visibility visibility,
        PostStatus status,
        List<PageNode> tree,
        AuthorSummaryV1Dto author) {}
