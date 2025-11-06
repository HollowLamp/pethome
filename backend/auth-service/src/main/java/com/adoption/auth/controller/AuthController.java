package com.adoption.auth.controller;

import com.adoption.auth.model.RegisterDTO;
import com.adoption.auth.model.UserAccount;
import com.adoption.auth.service.AuthService;
import com.adoption.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册接口
     * POST /auth/register
     */
    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterDTO user) {
        return authService.register(user);
    }

    /**
     * 发送注册验证码
     * POST /auth/register/code { email }
     */
    @PostMapping("/register/code")
    public ApiResponse<String> sendRegisterCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return authService.sendRegisterCode(email);
    }

    /**
     * 登录接口
     * POST /auth/login
     * 请求参数: username, password
     * 返回: JWT + 用户基本信息
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return authService.login(username, password);
    }

    /**
     * 分配角色
     * POST /auth/roles/assign
     * 请求体示例:
     * {
     *   "userId": 5,
     *   "role": "ADMIN"
     * }
     * 说明：角色校验由网关完成，这里只处理业务逻辑
     */
    @PostMapping("/roles/assign")
    public ApiResponse<String> assignRole(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String role = request.get("role").toString();
        return authService.assignRole(userId, role);
    }

    /**
     * 查询用户角色
     * GET /auth/users/{id}/roles
     * 说明：权限控制在网关完成
     */
    @GetMapping("/users/{id}/roles")
    public ApiResponse<List<String>> getUserRoles(@PathVariable("id") Long userId) {
        return authService.getUserRoles(userId);
    }
}
