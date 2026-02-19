package com.bamboo.postService.dto.doc;

import com.bamboo.postService.common.model.PageNode;
import com.bamboo.postService.entity.AuthorSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record DocResponse(
        UUID id,
        String title,
        String coverUrl,
        String description,
        String content,
        Set<String> tags,
        Instant createdAt,
        List<PageNode> tree,
        AuthorSnapshot authorProfile) {}
