package com.bamboo.postService.controller;

import com.bamboo.postService.dto.blog.BlogCursorResponseV1Dto;
import com.bamboo.postService.dto.doc.DocCursorResponseV1Dto;
import com.bamboo.postService.service.BlogQueryService;
import com.bamboo.postService.service.DocsQueryService;

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

    private final BlogQueryService blogQueryService;
    private final DocsQueryService docsQueryService;

    public InternalServiceController(
            BlogQueryService blogQueryService, DocsQueryService docsQueryService) {
        this.blogQueryService = blogQueryService;
        this.docsQueryService = docsQueryService;
    }

    @GetMapping("/blogs/user/{id}")
    public ResponseEntity<BlogCursorResponseV1Dto> getBlogForUser(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        if (cursor == null) cursor = Instant.now();
        return ResponseEntity.ok(blogQueryService.getForUser(id, cursor, pageable));
    }

    @GetMapping("/docs/user/{id}")
    public ResponseEntity<DocCursorResponseV1Dto> getDocsForUser(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        if (cursor == null) cursor = Instant.now();
        return ResponseEntity.ok(docsQueryService.getForUser(id, cursor, pageable));
    }
}
