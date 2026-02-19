package com.bamboo.postService.repository;

import com.bamboo.postService.entity.Tags;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tags, UUID> {
    List<Tags> findByTagIn(Collection<String> names);
}
