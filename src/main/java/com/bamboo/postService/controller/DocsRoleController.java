package com.bamboo.postService.controller;

import com.bamboo.postService.common.response.DocsMemberRoleResponse;
import com.bamboo.postService.common.response.RoleResponse;
import com.bamboo.postService.common.response.UpdateRoleRequest;
import com.bamboo.postService.common.response.UpsertRoleRequest;
import com.bamboo.postService.service.DocsRoleService;
import com.bamboo.postService.service.DocsService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/docs")
@RequiredArgsConstructor
public class DocsRoleController {

    private final DocsRoleService docsRoleService;
    private final DocsService docsService;

    @GetMapping("/role/{docsId}")
    public ResponseEntity<RoleResponse> getRole(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID docsId) {
        return docsService.getRole(userId, docsId);
    }

    @GetMapping("/{docsId}/roles")
    public ResponseEntity<List<DocsMemberRoleResponse>> listRoles(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID docsId) {
        return ResponseEntity.ok(docsRoleService.listRoles(userId, docsId));
    }

    @PostMapping("/{docsId}/roles")
    public ResponseEntity<RoleResponse> addRole(
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable UUID docsId,
            @RequestBody UpsertRoleRequest request) {

        return ResponseEntity.ok(docsRoleService.addRole(actorUserId, docsId, request));
    }

    @PatchMapping("/{docsId}/roles")
    public ResponseEntity<RoleResponse> updateRole(
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable UUID docsId,
            @RequestParam("targetEmail") String targetEmail,
            @RequestBody UpdateRoleRequest request) {

        return ResponseEntity.ok(
                docsRoleService.updateRole(actorUserId, docsId, targetEmail, request.role()));
    }

    @DeleteMapping("/{docsId}/roles")
    public ResponseEntity<RoleResponse> deleteRole(
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable UUID docsId,
            @RequestParam("targetEmail") String targetEmail) {

        return ResponseEntity.ok(docsRoleService.deleteRole(actorUserId, docsId, targetEmail));
    }
}
