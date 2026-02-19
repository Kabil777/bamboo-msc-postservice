package com.bamboo.postService.dto.doc;

import java.util.List;

public record DocCreateRequestDto(
        String title,
        String coverUrl,
        String description,
        String content,
        List<String> tags,
        List<DocPageRequestNode> pages) {}
