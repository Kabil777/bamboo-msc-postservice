package com.bamboo.postService.dto.blog;

import com.bamboo.postService.entity.AuthorSnapshot;

import java.util.List;

public record MetaPostDto(
        String title,
        String description,
        String coverUrl,
        List<String> tags,
        AuthorSnapshot authorSnapshot) {}
