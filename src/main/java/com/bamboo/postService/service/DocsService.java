package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.common.helper.DocsTrasformer;
import com.bamboo.postService.common.helper.DocsTrasformer.TransformResult;
import com.bamboo.postService.common.response.CommonResponse;
import com.bamboo.postService.dto.blog.MetaPostDto;
import com.bamboo.postService.dto.doc.DocCreateRequestDto;
import com.bamboo.postService.dto.doc.DocHomeDto;
import com.bamboo.postService.dto.doc.DocResponse;
import com.bamboo.postService.entity.AuthorSnapshot;
import com.bamboo.postService.entity.Docs;
import com.bamboo.postService.entity.Pages;
import com.bamboo.postService.entity.Tags;
import com.bamboo.postService.repository.DocsRepository;
import com.bamboo.postService.repository.PageRepository;
import com.bamboo.postService.repository.TagRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocsService extends BaseService {
    private final DocsRepository docsRepository;
    private final PageRepository pageRepository;
    private final TagRepository tagRepository;

    public DocsService(
            DocsRepository docsRepository,
            PageRepository pageRepository,
            TagRepository tagRepository) {
        this.docsRepository = docsRepository;
        this.pageRepository = pageRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public ResponseEntity<CommonResponse<String>> savePost(
            DocCreateRequestDto doc, AuthorSnapshot snapshot) {

        Docs docs = new Docs();
        docs.setId(UUID.randomUUID());
        docs.setTitle(doc.title());
        docs.setDescription(doc.description());
        docs.setCoverUrl(doc.coverUrl());
        docs.setAuthorSnapshot(snapshot);
        docs.setContent(doc.content());
        docs.setVisibility(Visibility.PRIVATE);
        docs.setStatus(PostStatus.DRAFT);

        List<Tags> existingTags = tagRepository.findByTagIn(doc.tags());

        Set<String> requestedTags = new HashSet<>(doc.tags());
        Set<String> foundTags = existingTags.stream().map(Tags::getTag).collect(Collectors.toSet());

        requestedTags.removeAll(foundTags);

        if (!requestedTags.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid tags: " + requestedTags);
        }
        docs.setTags(new HashSet<>(existingTags));

        TransformResult result = DocsTrasformer.transform(docs.getId(), doc.pages());
        docs.setTree(result.tree());
        docsRepository.save(docs);
        pageRepository.saveAll(result.pages());
        return buildResponse(HttpStatus.CREATED, "Docs created successfully !");
    }

    @Transactional
    public ResponseEntity<CommonResponse<Map<String, UUID>>> saveDocsMeta(
            MetaPostDto entity, AuthorSnapshot snapshot) {

        Docs doc =
                Docs.builder()
                        .title(entity.title())
                        .coverUrl(entity.coverUrl())
                        .description(entity.description())
                        .authorSnapshot(snapshot)
                        .createdAt(Instant.now())
                        .visibility(Visibility.PRIVATE)
                        .status(PostStatus.DRAFT)
                        .build();
        List<Tags> existingTags = tagRepository.findByTagIn(entity.tags());
        Set<Tags> tags = new HashSet<>(existingTags);
        doc.setTags(tags);

        docsRepository.save(doc);

        return buildResponse(HttpStatus.CREATED, Map.of("id", doc.getId()));
    }

    @Transactional
    public ResponseEntity<DocResponse> getDocAndContent(UUID id) {
        Docs docs =
                docsRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Document with current Id not found"));

        Set<String> tags = docs.getTags().stream().map(Tags::getTag).collect(Collectors.toSet());

        DocResponse response =
                new DocResponse(
                        docs.getId(),
                        docs.getTitle(),
                        docs.getCoverUrl(),
                        docs.getDescription(),
                        docs.getContent(),
                        tags,
                        docs.getCreatedAt(),
                        docs.getTree(),
                        docs.getAuthorSnapshot());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Transactional
    public ResponseEntity<CommonResponse<String>> getPageWithId(UUID pageId, UUID docId) {
        Pages page =
                pageRepository
                        .findById(pageId)
                        .orElseThrow(() -> new EntityNotFoundException("Page Not Found"));

        if (!page.getDocId().equals(docId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Page Signature Mismatch");
        }
        return buildResponse(HttpStatus.OK, page.getContent());
    }

    @Transactional
    public ResponseEntity<List<DocHomeDto>> getDocForHome(Pageable pageable) {
        List<DocHomeDto> coverDocs = docsRepository.findAllCoverDocs(pageable);
        return ResponseEntity.ok(coverDocs);
    }
}
