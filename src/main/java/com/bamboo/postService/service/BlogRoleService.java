package com.bamboo.postService.service;

import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.response.BlogMemberRoleResponse;
import com.bamboo.postService.common.response.RoleResponse;
import com.bamboo.postService.common.response.UpsertRoleRequest;
import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.BlogMember;
import com.bamboo.postService.exception.AccessDeniedException;
import com.bamboo.postService.exception.RoleNotFoundException;
import com.bamboo.postService.exception.UserNotFoundException;
import com.bamboo.postService.feign.UserServiceClient;
import com.bamboo.postService.repository.BlogRoleRepository;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BlogRoleService {

    private final BlogRoleRepository blogRoleRepository;
    private final UserServiceClient userServiceClient;

    public RoleResponse addRole(UUID actorUserId, UUID blogId, UpsertRoleRequest request) {
        if (request.userEmail() == null || request.userEmail().isBlank()) {
            throw new IllegalStateException("User email is required");
        }
        if (request.role() == null) {
            throw new IllegalStateException("Role is required");
        }
        log.info(
                "addRole request received blogId={} actorUserId={} userEmail={} role={}",
                blogId,
                actorUserId,
                request.userEmail(),
                request.role());

        UserMetaDto targetUser = resolveUserByEmail(request.userEmail());

        assertOwner(actorUserId, blogId);

        BlogMember member =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, targetUser.id())
                        .orElse(
                                BlogMember.builder()
                                        .blogId(blogId)
                                        .userId(targetUser.id())
                                        .role(request.role())
                                        .build());

        member.setRole(request.role());
        member.setUserName(targetUser.name());
        member.setUserHandle(targetUser.handle());
        member.setUserCoverUrl(targetUser.coverUrl());
        member.setUserEmail(targetUser.email());
        blogRoleRepository.save(member);

        return success(member.getRole(), "Role added/updated");
    }

    public RoleResponse updateRole(
            UUID actorUserId, UUID blogId, String targetEmail, Roles newRole) {
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalStateException("Target email is required");
        }
        if (newRole == null) {
            throw new IllegalStateException("Role is required");
        }
        UserMetaDto targetUser = resolveUserByEmail(targetEmail);

        assertOwner(actorUserId, blogId);

        BlogMember target =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, targetUser.id())
                        .orElseThrow(
                                () -> new RoleNotFoundException("Target membership not found"));

        if (target.getRole() == Roles.OWNER && newRole != Roles.OWNER) {
            assertNotLastOwner(blogId);
        }

        target.setRole(newRole);
        target.setUserName(targetUser.name());
        target.setUserHandle(targetUser.handle());
        target.setUserCoverUrl(targetUser.coverUrl());
        target.setUserEmail(targetUser.email());
        blogRoleRepository.save(target);

        return success(target.getRole(), "Role updated");
    }

    public RoleResponse deleteRole(UUID actorUserId, UUID blogId, String targetEmail) {
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalStateException("Target email is required");
        }
        UserMetaDto targetUser = resolveUserByEmail(targetEmail);

        assertOwner(actorUserId, blogId);

        BlogMember target =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, targetUser.id())
                        .orElseThrow(
                                () -> new RoleNotFoundException("Target membership not found"));

        if (target.getRole() == Roles.OWNER) {
            assertNotLastOwner(blogId);
        }

        blogRoleRepository.delete(target);
        return new RoleResponse(true, "REMOVED", false, HttpStatus.OK, "Role removed");
    }

    public List<BlogMemberRoleResponse> listRoles(UUID userId, UUID blogId) {
        // Any member can view roles list.
        blogRoleRepository
                .findByBlogIdAndUserId(blogId, userId)
                .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        return blogRoleRepository.findAllByBlogId(blogId).stream()
                .sorted(Comparator.comparing((BlogMember m) -> m.getRole() != Roles.OWNER))
                .map(
                        m ->
                                new BlogMemberRoleResponse(
                                        m.getUserId(),
                                        m.getUserName(),
                                        m.getUserHandle(),
                                        m.getUserCoverUrl(),
                                        m.getUserEmail(),
                                        m.getRole().name()))
                .toList();
    }

    private UserMetaDto resolveUserByEmail(String email) {
        try {
            return userServiceClient.getUserByEmail(email);
        } catch (Exception ex) {
            throw new UserNotFoundException("User not found: " + email);
        }
    }

    private void assertOwner(UUID actorUserId, UUID blogId) {
        BlogMember actor =
                blogRoleRepository
                        .findByBlogIdAndUserId(blogId, actorUserId)
                        .orElseThrow(() -> new RoleNotFoundException("Unauthorized"));

        if (actor.getRole() != Roles.OWNER) {
            throw new AccessDeniedException("Only owner can manage roles");
        }
    }

    private void assertNotLastOwner(UUID blogId) {
        long ownerCount = blogRoleRepository.countByBlogIdAndRole(blogId, Roles.OWNER);
        if (ownerCount <= 1) {
            throw new IllegalStateException("Cannot remove/downgrade last owner");
        }
    }

    private RoleResponse success(Roles role, String msg) {
        return new RoleResponse(true, role.name(), role == Roles.READER, HttpStatus.OK, msg);
    }
}
