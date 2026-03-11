package com.bamboo.postService.dto.blog;

import java.util.List;

public record MetaPostDto(String title, String description, String coverUrl, List<String> tags) {}
