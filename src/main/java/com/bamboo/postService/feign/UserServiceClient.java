package com.bamboo.postService.feign;

import com.bamboo.postService.dto.feign.UserMetaDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service-client", url = "${user.service.url}")
public interface UserServiceClient {
    @GetMapping("/internal/user")
    UserMetaDto getUserByEmail(@RequestParam("email") String email);
}
