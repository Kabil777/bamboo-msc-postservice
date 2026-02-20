package com.bamboo.postService.controller;

import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.response.CommonResponse;
import com.bamboo.postService.dto.blog.BlogDetailsDto;
import com.bamboo.postService.dto.blog.BlogPageDto;
import com.bamboo.postService.dto.blog.CursorResponse;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.entity.AuthorSnapshot;
import com.bamboo.postService.service.BlogService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/blog", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping("/{blogId}/content")
    public ResponseEntity<CommonResponse<String>> saveContent(
            @PathVariable UUID blogId, @RequestBody BlogPageDto blogPageDto) {
        log.info("data: {}", blogPageDto.content());
        return blogService.saveContent(
                blogId,
                blogPageDto.content(),
                blogPageDto.visibility(),
                blogPageDto.status());
    }

    @PostMapping("/{blogId}/visibility")
    public ResponseEntity<CommonResponse<String>> updateVisibility(
            @PathVariable UUID blogId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody VisibilityUpdateRequest request) {
        return blogService.updateVisibility(blogId, userId, request);
    }

    @PostMapping("/meta")
    public ResponseEntity<CommonResponse<Map<String, UUID>>> savePost(
            @RequestBody MetaPostDto entity,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Name") String name,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Handle", required = false) String handle,
            @RequestHeader(value = "X-User-Avatar", required = false) String avatarUrl) {
        AuthorSnapshot snapshot = new AuthorSnapshot(userId, name, handle, avatarUrl);

        return blogService.save(entity, snapshot);
    }

    @GetMapping
    public ResponseEntity<CursorResponse> getPages(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @RequestParam(required = false) Instant cursor) {
        if (cursor == null) cursor = Instant.now();
        return blogService.getCoverBlogs(pageable, cursor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogDetailsDto> getBlogContent(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return blogService.getBlogById(id, userId);
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<CursorResponse> getBlogWithTag(
            @PathVariable String tag,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        if (cursor == null) cursor = Instant.now();
        return blogService.getByTags(cursor, tag, pageable);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<CursorResponse> getBlogByUser(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @RequestParam(required = false) Visibility visibility) {
        if (cursor == null) cursor = Instant.now();
        return blogService.getByUser(id, cursor, pageable, visibility);
    }
}
