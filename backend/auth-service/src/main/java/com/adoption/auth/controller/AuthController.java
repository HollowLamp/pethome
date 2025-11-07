package com.adoption.auth.controller;

import com.adoption.auth.model.RegisterDTO;
import com.adoption.auth.model.UserAccount;
import com.adoption.auth.service.AuthService;
import com.adoption.common.api.ApiResponse;
import com.adoption.common.service.FileService;
import com.adoption.common.util.FileUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final FileService fileService;

    public AuthController(AuthService authService, FileService fileService) {
        this.authService = authService;
        this.fileService = fileService;
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

    /**
     * 查询用户列表（分页）
     * GET /auth/users?page=1&pageSize=10
     * 说明：仅超级管理员可访问
     */
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUserList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return authService.getUserList(page, pageSize);
    }

    /**
     * 删除用户角色
     * DELETE /auth/roles/remove
     * 请求体示例:
     * {
     *   "userId": 5,
     *   "role": "AUDITOR"
     * }
     * 说明：仅超级管理员可访问
     */
    @DeleteMapping("/roles/remove")
    public ApiResponse<String> removeRole(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String role = request.get("role").toString();
        return authService.removeRole(userId, role);
    }

    /**
     * 获取当前用户信息
     * GET /auth/me
     * 说明：需要登录，从JWT中获取userId
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        return authService.getCurrentUser(userId);
    }

    /**
     * 更新用户信息
     * PUT /auth/me
     * 请求体示例:
     * {
     *   "username": "newusername",
     *   "email": "newemail@example.com",
     *   "phone": "13800138000",
     *   "avatarUrl": "user/2024-01-15/uuid.jpg"
     * }
     * 说明：需要登录，从JWT中获取userId
     */
    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateUser(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String phone = body.get("phone");
        String avatarUrl = body.get("avatarUrl");
        return authService.updateUser(userId, username, email, phone, avatarUrl);
    }

    /**
     * 更新密码
     * PUT /auth/me/password
     * 请求体示例:
     * {
     *   "oldPassword": "oldpass",
     *   "newPassword": "newpass"
     * }
     * 说明：需要登录，从JWT中获取userId
     */
    @PutMapping("/me/password")
    public ApiResponse<String> updatePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        return authService.updatePassword(userId, oldPassword, newPassword);
    }

    /**
     * 上传头像
     * POST /auth/me/avatar
     * 说明：需要登录，从JWT中获取userId
     */
    @PostMapping("/me/avatar")
    public ApiResponse<Map<String, Object>> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return ApiResponse.error(400, "文件不能为空");
            }

            // 检查文件类型
            if (!FileUtils.isImage(file.getOriginalFilename())) {
                return ApiResponse.error(400, "只支持图片文件");
            }

            // 上传文件
            InputStream inputStream = FileUtils.toInputStream(file);
            FileService.FileInfo fileInfo = fileService.uploadFile(
                    inputStream,
                    file.getOriginalFilename(),
                    "user"  // 用户头像分类
            );

            // 更新用户头像
            ApiResponse<Map<String, Object>> updateResult = authService.updateUser(
                    userId, null, null, null, fileInfo.getRelativePath()
            );

            if (updateResult.getCode() == 200) {
                Map<String, Object> result = updateResult.getData();
                result.put("avatarUrl", fileInfo.getUrl()); // 返回完整URL
                return ApiResponse.success(result);
            }

            return updateResult;

        } catch (Exception e) {
            return ApiResponse.error(500, "头像上传失败: " + e.getMessage());
        }
    }
}
