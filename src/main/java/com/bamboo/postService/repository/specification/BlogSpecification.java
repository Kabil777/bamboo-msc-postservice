package com.bamboo.postService.repository.specification;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;
import com.bamboo.postService.entity.Blog;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class BlogSpecification {
    private BlogSpecification() {}

    public static Specification<Blog> hasAuthor(UUID authorId) {
        return (root, qurey, cb) ->
                authorId == null ? null : cb.equal(root.get("authorId"), authorId);
    }

    public static Specification<Blog> hasVisibility(Visibility visibility) {
        return (root, qurey, cb) ->
                visibility == null ? null : cb.equal(root.get("visibility"), visibility);
    }

    public static Specification<Blog> hasStatus(PostStatus status) {
        return (root, qurey, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
