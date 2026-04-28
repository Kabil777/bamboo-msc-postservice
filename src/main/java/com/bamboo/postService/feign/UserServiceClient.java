package com.bamboo.postService.feign;

import com.bamboo.postService.dto.feign.UserMetaDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service-client", url = "${user.service.url}")
public interface UserServiceClient {
    @GetMapping("/internal/user")
    UserMetaDto getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/internal/user/{id}")
    UserMetaDto getUserById(@PathVariable("id") UUID id);

    @PostMapping("/internal/users/lookup")
    List<UserMetaDto> getUsersByIds(@RequestBody List<UUID> ids);
}
