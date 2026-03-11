package com.bamboo.postService.service;

import com.bamboo.postService.dto.feign.UserMetaDto;
import com.bamboo.postService.entity.AuthorProfileProjection;
import com.bamboo.postService.repository.AuthorProfileProjectionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorProjectionService {

    private final AuthorProfileProjectionRepository authorProfileProjectionRepository;

    public AuthorProjectionService(
            AuthorProfileProjectionRepository authorProfileProjectionRepository) {
        this.authorProfileProjectionRepository = authorProfileProjectionRepository;
    }

    @Transactional
    public AuthorProfileProjection upsert(UserMetaDto user) {
        AuthorProfileProjection projection =
                authorProfileProjectionRepository
                        .findById(user.id())
                        .orElseGet(() -> AuthorProfileProjection.builder().id(user.id()).build());

        projection.setName(user.name());
        projection.setHandle(user.handle());
        projection.setAvatarUrl(user.coverUrl());
        return authorProfileProjectionRepository.save(projection);
    }
}
