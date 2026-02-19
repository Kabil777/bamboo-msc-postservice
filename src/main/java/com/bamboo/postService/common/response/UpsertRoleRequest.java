package com.bamboo.postService.common.response;

import com.bamboo.postService.common.enums.Roles;

public record UpsertRoleRequest(String userEmail, Roles role) {}
