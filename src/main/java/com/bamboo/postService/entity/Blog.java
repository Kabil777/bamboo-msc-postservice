package com.bamboo.postService.entity;

import com.bamboo.postService.common.enums.PostStatus;
import com.bamboo.postService.common.enums.Visibility;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "blogs",
        indexes = {
            @Index(name = "idx_posts_creted_at", columnList = "createdAt"),
            @Index(name = "idx_author_id", columnList = "authorId")
        })
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private String coverUrl;

    @Column(nullable = false)
    private String description;

    @OneToOne(
            mappedBy = "post",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private BlogContent content;

    @Embedded private AuthorSnapshot authorSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_tag_mapper",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            indexes = {
                @Index(name = "idx_post_tag_post_id", columnList = "post_id"),
                @Index(name = "idx_post_tag_tag_id", columnList = "tag_id"),
                @Index(name = "idx_post_tag_tag_post", columnList = "tag_id, post_id")
            })
    private Set<Tags> tags = new HashSet<>();

    @Column(nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp private Instant updatedAt;
}
