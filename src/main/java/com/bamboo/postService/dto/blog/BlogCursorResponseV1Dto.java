package com.bamboo.postService.dto.blog;

import java.time.Instant;
import java.util.List;

public record BlogCursorResponseV1Dto(
        List<BlogFeedItemV1Dto> items, Boolean hasNext, Instant cursor) {}
