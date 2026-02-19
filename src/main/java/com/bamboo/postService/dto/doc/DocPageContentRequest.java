package com.bamboo.postService.dto.doc;

import java.util.UUID;

public record DocPageContentRequest(UUID pageId, String markdown) {}
