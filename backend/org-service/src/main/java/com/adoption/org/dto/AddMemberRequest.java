package com.adoption.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 针对机构管理员添加机构成员时使用的请求参数结构体
 * userId: 指定要加入该机构的用户ID
 * role: 指定用户在该机构内的角色，例如 ADMIN / REVIEWER 等
 */
public class AddMemberRequest {

    // 要加入机构的用户ID（由 auth-service 分配）
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    // 该用户在机构内扮演的角色，例如 ADMIN, REVIEWER 等
    @NotBlank(message = "角色不能为空")
    private String role;

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}
