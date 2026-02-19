package com.bamboo.postService.dto.blog;

import java.time.Instant;
import java.util.List;

public record CursorResponse(List<BlogPagesDto> blogPagesDto, Boolean hasNext, Instant cursor) {}
