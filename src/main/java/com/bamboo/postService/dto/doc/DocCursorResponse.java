package com.bamboo.postService.dto.doc;

import java.time.Instant;
import java.util.List;

public record DocCursorResponse(List<DocHomeDto> docs, Boolean hasNext, Instant cursor) {}
