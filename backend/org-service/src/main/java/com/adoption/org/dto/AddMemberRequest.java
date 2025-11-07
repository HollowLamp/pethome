package com.adoption.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 针对机构管理员添加机构成员时使用的请求参数结构体
 * userId: 指定要加入该机构的用户ID
 */
public class AddMemberRequest {

    // 要加入机构的用户ID（由 auth-service 分配）
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
