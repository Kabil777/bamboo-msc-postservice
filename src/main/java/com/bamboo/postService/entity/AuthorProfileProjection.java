package com.bamboo.postService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "author_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorProfileProjection {
    @Id private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String handle;

    @Column(length = 255)
    private String avatarUrl;

    @UpdateTimestamp private Instant updatedAt;
}
