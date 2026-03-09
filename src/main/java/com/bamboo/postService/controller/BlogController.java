package com.bamboo.postService.controller;

import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.response.CommonResponse;
import com.bamboo.postService.dto.blog.BlogCursorResponseV1Dto;
import com.bamboo.postService.dto.blog.BlogDetailV1Dto;
import com.bamboo.postService.dto.blog.BlogFeedItemV1Dto;
import com.bamboo.postService.dto.blog.BlogPageDto;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.service.BlogCommandService;
import com.bamboo.postService.service.BlogQueryService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/blog", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class BlogController {

    private final BlogCommandService blogCommandService;
    private final BlogQueryService blogQueryService;

    public BlogController(
            BlogCommandService blogCommandService, BlogQueryService blogQueryService) {
        this.blogCommandService = blogCommandService;
        this.blogQueryService = blogQueryService;
    }

    @PostMapping("/{blogId}/content")
    public ResponseEntity<CommonResponse<String>> saveContent(
            @PathVariable UUID blogId,
            @RequestBody BlogPageDto blogPageDto,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new CommonResponse<>(
                                HttpStatus.CREATED.value(),
                                blogCommandService.saveContent(
                                        userId,
                                        blogId,
                                        blogPageDto.content(),
                                        blogPageDto.visibility(),
                                        blogPageDto.status()),
                                Instant.now()));
    }

    @PostMapping("/{blogId}/visibility")
    public ResponseEntity<CommonResponse<String>> updateVisibility(
            @PathVariable UUID blogId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody VisibilityUpdateRequest request) {
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(),
                        blogCommandService.updateVisibility(blogId, userId, request),
                        Instant.now()));
    }

    @PostMapping("/meta")
    public ResponseEntity<CommonResponse<Map<String, UUID>>> savePost(
            @RequestBody MetaPostDto entity, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new CommonResponse<>(
                                HttpStatus.CREATED.value(),
                                blogCommandService.save(entity, userId),
                                Instant.now()));
    }

    @GetMapping
    public ResponseEntity<BlogCursorResponseV1Dto> getPages(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @RequestParam(required = false) Instant cursor) {
        if (cursor == null) cursor = Instant.now();
        BlogCursorResponseV1Dto response = blogQueryService.getCoverBlogs(pageable, cursor);
        if (response.items().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<BlogFeedItemV1Dto>> getFeaturedPages(
            @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(blogQueryService.getFeaturedBlogs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogDetailV1Dto> getBlogContent(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(blogQueryService.getBlogById(id, userId));
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<BlogCursorResponseV1Dto> getBlogWithTag(
            @PathVariable String tag,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        if (cursor == null) cursor = Instant.now();
        BlogCursorResponseV1Dto response = blogQueryService.getByTags(cursor, tag, pageable);
        if (response.items().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<BlogCursorResponseV1Dto> getBlogByUser(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @RequestParam(required = false) Visibility visibility,
            HttpServletRequest request) {
        if (cursor == null) cursor = Instant.now();
        return ResponseEntity.ok(
                blogQueryService.getByUser(
                        id, cursor, pageable, visibility, request.getHeader("X-User-Id")));
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<CommonResponse<String>> deleteBlogById(
            @PathVariable UUID blogId, @RequestHeader("X-User-Id") UUID userId) {
        blogCommandService.deleteById(blogId, userId);
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(), "Blog deleted successfully", Instant.now()));
    }
}
