package com.bamboo.postService.controller;

import com.bamboo.postService.common.response.CommonResponse;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.doc.DocCreateRequestDto;
import com.bamboo.postService.dto.doc.DocHomeDto;
import com.bamboo.postService.dto.doc.DocResponse;
import com.bamboo.postService.entity.AuthorSnapshot;
import com.bamboo.postService.service.DocsService;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/docs", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocsController {

    private final DocsService docsService;

    public DocsController(DocsService docsService) {
        this.docsService = docsService;
    }

    @GetMapping
    public ResponseEntity<List<DocHomeDto>> getCoverDocs(
            @PageableDefault(size = 4, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return docsService.getDocForHome(pageable);
    }

    @PostMapping("/meta")
    public ResponseEntity<CommonResponse<Map<String, UUID>>> saveDocsMeta(
            @RequestBody MetaPostDto entity,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Name") String name,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Handle", required = false) String handle,
            @RequestHeader(value = "X-User-Avatar", required = false) String avatarUrl) {

        AuthorSnapshot snapshot = new AuthorSnapshot(userId, name, handle, avatarUrl);
        return docsService.saveDocsMeta(entity, snapshot);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocResponse> getById(@PathVariable("id") String id) {
        return docsService.getDocAndContent(UUID.fromString(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse<String>> createDocs(
            @RequestBody DocCreateRequestDto document,
            @RequestBody MetaPostDto entity,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Name") String name,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Handle", required = false) String handle,
            @RequestHeader(value = "X-User-Avatar", required = false) String avatarUrl) {

        AuthorSnapshot snapshot = new AuthorSnapshot(userId, name, handle, avatarUrl);
        return docsService.savePost(document, snapshot);
    }

    @GetMapping("/{docId}/{pageId}")
    public ResponseEntity<CommonResponse<String>> getPageContent(
            @PathVariable("docId") String docId, @PathVariable("pageId") String pageId) {
        return docsService.getPageWithId(UUID.fromString(pageId), UUID.fromString(docId));
    }
}
