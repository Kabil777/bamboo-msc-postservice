package com.bamboo.postService.repository;

import com.bamboo.postService.entity.Pages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Pages, UUID> {}
