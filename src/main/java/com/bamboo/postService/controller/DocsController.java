package com.bamboo.postService.controller;

import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.response.CommonResponse;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.common.VisibilityUpdateRequest;
import com.bamboo.postService.dto.doc.DocCreateRequestDto;
import com.bamboo.postService.dto.doc.DocCursorResponseV1Dto;
import com.bamboo.postService.dto.doc.DocDetailV1Dto;
import com.bamboo.postService.dto.doc.DocFeedItemV1Dto;
import com.bamboo.postService.dto.doc.DocsContentRequest;
import com.bamboo.postService.service.DocsCommandService;
import com.bamboo.postService.service.DocsQueryService;

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

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/docs", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocsController {

    private final DocsCommandService docsCommandService;
    private final DocsQueryService docsQueryService;

    public DocsController(
            DocsCommandService docsCommandService, DocsQueryService docsQueryService) {
        this.docsCommandService = docsCommandService;
        this.docsQueryService = docsQueryService;
    }

    @GetMapping
    public ResponseEntity<List<DocFeedItemV1Dto>> getCoverDocs(
            @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ResponseEntity.ok(docsQueryService.getDocForHome(pageable));
    }

    @PostMapping("/meta")
    public ResponseEntity<CommonResponse<Map<String, UUID>>> saveDocsMeta(
            @RequestBody MetaPostDto entity, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new CommonResponse<>(
                                HttpStatus.CREATED.value(),
                                docsCommandService.saveDocsMeta(entity, userId),
                                Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocDetailV1Dto> getById(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(docsQueryService.getDocAndContent(UUID.fromString(id), userId));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<DocCursorResponseV1Dto> getDocsByUser(
            @PathVariable UUID id,
            @RequestParam(required = false) Instant cursor,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            @RequestParam(required = false) Visibility visibility,
            HttpServletRequest request) {
        if (cursor == null) cursor = Instant.now();
        return ResponseEntity.ok(
                docsQueryService.getByUser(
                        id, cursor, pageable, visibility, request.getHeader("X-User-Id")));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<String>> createDocs(
            @RequestBody DocCreateRequestDto document, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new CommonResponse<>(
                                HttpStatus.CREATED.value(),
                                docsCommandService.savePost(document, userId),
                                Instant.now()));
    }

    @PostMapping("/{docId}/content")
    public ResponseEntity<CommonResponse<String>> saveDocsContent(
            @PathVariable("docId") String docId,
            @RequestBody DocsContentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(),
                        docsCommandService.saveDocsContent(userId, UUID.fromString(docId), request),
                        Instant.now()));
    }

    @PostMapping("/{docId}/visibility")
    public ResponseEntity<CommonResponse<String>> updateVisibility(
            @PathVariable("docId") String docId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody VisibilityUpdateRequest request) {
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(),
                        docsCommandService.updateVisibility(UUID.fromString(docId), userId, request),
                        Instant.now()));
    }

    @PostMapping("/{docId}/content/save")
    public ResponseEntity<CommonResponse<String>> saveDocsContentUpload(
            @PathVariable("docId") String docId,
            @RequestBody DocsContentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(),
                        docsCommandService.saveDocsContent(userId, UUID.fromString(docId), request),
                        Instant.now()));
    }

    @GetMapping("/{docId}/{pageId}")
    public ResponseEntity<CommonResponse<String>> getPageContent(
            @PathVariable("docId") String docId,
            @PathVariable("pageId") String pageId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(),
                        docsQueryService.getPageWithId(
                                UUID.fromString(pageId), UUID.fromString(docId), userId),
                        Instant.now()));
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<CommonResponse<String>> deleteDocsById(
            @PathVariable("docId") String docId, @RequestHeader("X-User-Id") UUID userId) {
        docsCommandService.deleteById(UUID.fromString(docId), userId);
        return ResponseEntity.ok(
                new CommonResponse<>(
                        HttpStatus.OK.value(), "Docs deleted successfully", Instant.now()));
    }
}
