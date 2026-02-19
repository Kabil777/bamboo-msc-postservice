package com.bamboo.postService.repository;

import com.bamboo.postService.entity.BlogMember;
import com.bamboo.postService.common.enums.Roles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlogRoleRepository extends JpaRepository<BlogMember, UUID> {
    @Query(
            """
                Select b from BlogMember  b
                where b.userId = :userId and b.blogId = :blogId
            """)
    public Optional<BlogMember> findByBlogIdAndUserId(UUID blogId, UUID userId);

    long countByBlogIdAndRole(UUID blogId, Roles role);

    List<BlogMember> findAllByBlogId(UUID blogId);
}
