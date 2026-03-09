package com.bamboo.postService.policy;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.exception.AccessDeniedException;
import com.bamboo.postService.exception.RoleNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class PostAccessPolicy {

    public void assertCanView(Visibility visibility, PostStatus status, Roles memberRole) {
        if (visibility == Visibility.PUBLIC && status == PostStatus.PUBLISHED) {
            return;
        }

        if (memberRole != null) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    public void assertCanEdit(Roles memberRole) {
        Roles role = requireMemberRole(memberRole);
        if (role == Roles.OWNER || role == Roles.EDITOR) {
            return;
        }

        throw new AccessDeniedException("No write access");
    }

    public void assertCanManage(Roles memberRole, String message) {
        Roles role = requireMemberRole(memberRole);
        if (role == Roles.OWNER) {
            return;
        }

        throw new AccessDeniedException(message);
    }

    public void assertIsMember(Roles memberRole, String message) {
        if (memberRole == null) {
            throw new RoleNotFoundException(message);
        }
    }

    public Visibility resolveRequestedVisibility(
            UUID resourceOwnerId, Visibility requestedVisibility, String requesterIdHeader) {
        if (requestedVisibility == null || requestedVisibility == Visibility.PUBLIC) {
            return Visibility.PUBLIC;
        }

        UUID requesterId = parseRequesterId(requesterIdHeader);
        if (!resourceOwnerId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        return requestedVisibility;
    }

    private Roles requireMemberRole(Roles memberRole) {
        if (memberRole == null) {
            throw new RoleNotFoundException("Unauthorized");
        }

        return memberRole;
    }

    private UUID parseRequesterId(String requesterIdHeader) {
        if (requesterIdHeader == null || requesterIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        try {
            return UUID.fromString(requesterIdHeader);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
}
