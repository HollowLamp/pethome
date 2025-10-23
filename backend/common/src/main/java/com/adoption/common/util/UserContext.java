package com.adoption.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class UserContext {

    private final HttpServletRequest request;

    public UserContext(HttpServletRequest request) {
        this.request = request;
    }

    public Long getCurrentUserId() {
        String userId = request.getHeader("X-User-Id");
        return userId != null ? Long.valueOf(userId) : null;
    }

    public List<String> getCurrentUserRoles() {
        String rolesHeader = request.getHeader("X-Roles");
        return rolesHeader != null ? Arrays.asList(rolesHeader.split(",")) : List.of();
    }
}
