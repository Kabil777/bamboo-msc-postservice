package com.bamboo.postService.repository;

import com.bamboo.postService.entity.DocsMember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocsRoleRepository extends JpaRepository<DocsMember, UUID> {}
