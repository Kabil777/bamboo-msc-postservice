package com.bamboo.postService.entity;

import com.bamboo.postService.common.enums.Roles;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(
        name = "docs_roles",
        indexes = {
            @Index(name = "idx_docs_role_docsId", columnList = "docsId"),
            @Index(name = "idx_docs_role_userId", columnList = "userId")
        })
public class DocsMember {
    @Id private UUID docsId;

    @Nonnull private UUID userId;

    @Nonnull private Roles role;
}
