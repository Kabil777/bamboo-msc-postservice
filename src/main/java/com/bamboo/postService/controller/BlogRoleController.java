package com.bamboo.postService.controller;

import com.bamboo.postService.common.response.RoleResponse;
import com.bamboo.postService.common.response.UpdateRoleRequest;
import com.bamboo.postService.common.response.UpsertRoleRequest;
import com.bamboo.postService.common.response.BlogMemberRoleResponse;
import com.bamboo.postService.service.BlogRoleService;
import com.bamboo.postService.service.BlogService;

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
@RequestMapping("/api/v1/blog")
@RequiredArgsConstructor
public class BlogRoleController {

    private final BlogRoleService blogRoleService;
    private final BlogService blogService;

    @GetMapping("/role/{blogId}")
    public ResponseEntity<RoleResponse> getRole(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID blogId) {

        return blogService.getRole(userId, blogId);
    }

    @GetMapping("/{blogId}/roles")
    public ResponseEntity<List<BlogMemberRoleResponse>> listRoles(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID blogId) {
        return ResponseEntity.ok(blogRoleService.listRoles(userId, blogId));
    }

    @PostMapping("/{blogId}/roles")
    public ResponseEntity<RoleResponse> addRole(
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable UUID blogId,
            @RequestBody UpsertRoleRequest request) {

        return ResponseEntity.ok(blogRoleService.addRole(actorUserId, blogId, request));
    }

    @PatchMapping("/{blogId}/roles")
    public ResponseEntity<RoleResponse> updateRole(
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable UUID blogId,
            @RequestParam("targetEmail") String targetEmail,
            @RequestBody UpdateRoleRequest request) {

        return ResponseEntity.ok(
                blogRoleService.updateRole(actorUserId, blogId, targetEmail, request.role()));
    }

    @DeleteMapping("/{blogId}/roles")
    public ResponseEntity<RoleResponse> deleteRole(
            @RequestHeader("X-User-Id") UUID actorUserId,
            @PathVariable UUID blogId,
            @RequestParam("targetEmail") String targetEmail) {

        return ResponseEntity.ok(blogRoleService.deleteRole(actorUserId, blogId, targetEmail));
    }
}
