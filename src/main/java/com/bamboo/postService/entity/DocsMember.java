package com.bamboo.postService.entity;

import com.bamboo.postService.common.enums.Roles;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "docs_roles",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_blog_roles_docs_user",
                    columnNames = {"docsId", "userId"})
        },
        indexes = {
            @Index(name = "idx_blog_role_blogId", columnList = "blogId"),
            @Index(name = "idx_blog_role_userId", columnList = "userId")
        })
public class DocsMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Nonnull private UUID docsId;

    @Nonnull private UUID userId;

    private String userName;

    private String userHandle;

    private String userCoverUrl;

    private String userEmail;

    @Nonnull private Roles role;
}
