package com.bamboo.postService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pages", indexes = @Index(name = "idx_pages_doc_id", columnList = "docId, pageId"))
public class Pages {
    @Id private UUID pageId;

    @Column(nullable = false)
    UUID docId;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;
}
