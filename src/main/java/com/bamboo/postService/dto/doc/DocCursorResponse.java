package com.bamboo.postService.dto.doc;

import java.time.Instant;

public record DocCursorResponse(DocHomeDto docs, Boolean hasNext, Instant cursor) {}
