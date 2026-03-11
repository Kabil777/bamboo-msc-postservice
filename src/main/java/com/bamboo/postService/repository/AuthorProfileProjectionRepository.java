package com.bamboo.postService.repository;

import com.bamboo.postService.entity.AuthorProfileProjection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthorProfileProjectionRepository
        extends JpaRepository<AuthorProfileProjection, UUID> {}
