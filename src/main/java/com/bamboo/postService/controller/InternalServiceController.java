package com.bamboo.postService.controller;

import com.bamboo.postService.dto.blog.CursorResponse;
import com.bamboo.postService.service.BlogService;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(value = "/internal")
public class InternalServiceController {

    private final BlogService blogService;

    public InternalServiceController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/blogs/user/{id}")
    public ResponseEntity<CursorResponse> getBlogForUser(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        if (cursor == null) cursor = Instant.now();
        return blogService.getForUser(id, cursor, pageable);
    }
}
