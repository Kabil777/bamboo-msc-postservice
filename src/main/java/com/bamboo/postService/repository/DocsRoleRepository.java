package com.bamboo.postService.repository;

import com.bamboo.postService.common.enums.Roles;
import com.bamboo.postService.entity.DocsMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocsRoleRepository extends JpaRepository<DocsMember, UUID> {
    @Query(
            """
                Select d from DocsMember d
                where d.userId = :userId and d.docsId = :docsId
            """)
    Optional<DocsMember> findByDocsIdAndUserId(UUID docsId, UUID userId);

    long countByDocsIdAndRole(UUID docsId, Roles role);

    List<DocsMember> findAllByDocsId(UUID docsId);
}
