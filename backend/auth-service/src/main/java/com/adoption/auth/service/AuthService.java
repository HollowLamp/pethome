package com.adoption.auth.service;

import com.adoption.auth.model.RegisterDTO;
import com.adoption.auth.model.UserAccount;
import com.adoption.auth.repository.UserMapper;
import com.adoption.auth.repository.UserRoleMapper;
import com.adoption.common.api.ApiResponse;
import com.adoption.common.constant.RoleEnum;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final UserRoleMapper roleMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final StringRedisTemplate stringRedisTemplate;
    private final MailService mailService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(UserMapper userMapper, UserRoleMapper roleMapper,
                       StringRedisTemplate stringRedisTemplate, MailService mailService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.mailService = mailService;
    }

    /**
     * 注册：新用户默认分配 USER 角色
     */
    public ApiResponse<String> register(RegisterDTO dto) {
        // 校验邮箱验证码
        String key = buildRegisterCodeKey(dto.getEmail());
        String cachedCode = stringRedisTemplate.opsForValue().get(key);
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            return ApiResponse.error(4002, "验证码错误或已过期");
        }

        // 先查是否存在
        UserAccount existing = userMapper.findByUsername(dto.getUsername());
        if (existing != null) {
            return ApiResponse.error(4001, "用户名已存在");
        }

        // 构造实体对象
        UserAccount user = new UserAccount();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(encoder.encode(dto.getPassword())); // ✅ 后端做加密
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus("ACTIVE");

        // 插入用户
        userMapper.insert(user);

        // 默认分配 USER 角色
        roleMapper.insertUserRole(user.getId(), RoleEnum.USER.name());

        // 注册成功后删除验证码
        stringRedisTemplate.delete(key);

        return ApiResponse.success("注册成功");
    }



    /**
     * 登录：返回 JWT 和基本信息
     */
    public ApiResponse<Map<String, Object>> login(String username, String password) {
        UserAccount user = userMapper.findByUsername(username);
        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        // 查角色
        List<String> roles = roleMapper.findRolesByUserId(user.getId());

        // 生成 JWT
        String token = generateToken(user, roles);

        // 返回
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("roles", roles);

        return ApiResponse.success(result);
    }

    /**
     * 分配角色
     */
    public ApiResponse<String> assignRole(Long userId, String role) {
        List<String> roles = roleMapper.findRolesByUserId(userId);
        if (roles.contains(role)) {
            return ApiResponse.error(400, "用户已拥有该角色");
        }
        roleMapper.insertUserRole(userId, role);
        return ApiResponse.success("角色分配成功");
    }

    /**
     * 查询用户角色
     */
    public ApiResponse<List<String>> getUserRoles(Long userId) {
        List<String> roles = roleMapper.findRolesByUserId(userId);
        return ApiResponse.success(roles);
    }

    /**
     * 查询用户列表（分页）
     */
    public ApiResponse<Map<String, Object>> getUserList(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<UserAccount> users = userMapper.findAll(offset, pageSize);
        int total = userMapper.countAll();

        // 为每个用户查询角色
        List<Map<String, Object>> userList = new ArrayList<>();
        for (UserAccount user : users) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("status", user.getStatus());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("roles", roleMapper.findRolesByUserId(user.getId()));
            userList.add(userMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 删除用户角色
     */
    public ApiResponse<String> removeRole(Long userId, String role) {
        List<String> roles = roleMapper.findRolesByUserId(userId);
        if (!roles.contains(role)) {
            return ApiResponse.error(400, "用户不拥有该角色");
        }
        // 不能删除ADMIN角色
        if ("ADMIN".equals(role)) {
            return ApiResponse.error(400, "不能删除超级管理员角色");
        }
        // 不能删除最后一个角色
        if (roles.size() <= 1) {
            return ApiResponse.error(400, "不能删除用户的最后一个角色");
        }
        roleMapper.deleteUserRole(userId, role);
        return ApiResponse.success("角色移除成功");
    }

    private String generateToken(UserAccount user, List<String> roles) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    // 发送注册验证码
    public ApiResponse<String> sendRegisterCode(String email) {
        if (email == null || email.isEmpty()) {
            return ApiResponse.error(400, "邮箱不能为空");
        }
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        String key = buildRegisterCodeKey(email);
        // 缓存 5 分钟
        stringRedisTemplate.opsForValue().set(key, code, java.time.Duration.ofMinutes(5));
        // 发送邮件
        mailService.sendPlainText(email, "注册验证码", "您的注册验证码为：" + code + "（5分钟内有效）");
        return ApiResponse.success("验证码已发送");
    }

    private String buildRegisterCodeKey(String email) {
        return "auth:register:code:" + email;
    }

    /**
     * 获取当前用户信息
     */
    public ApiResponse<Map<String, Object>> getCurrentUser(Long userId) {
        UserAccount user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        List<String> roles = roleMapper.findRolesByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("roles", roles);

        return ApiResponse.success(result);
    }

    /**
     * 根据用户ID获取用户信息（用于跨服务调用）
     */
    public ApiResponse<Map<String, Object>> getUserById(Long userId) {
        UserAccount user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatarUrl", user.getAvatarUrl());

        return ApiResponse.success(result);
    }

    /**
     * 更新用户信息（不包括密码）
     */
    public ApiResponse<Map<String, Object>> updateUser(Long userId, String username, String email, String phone, String avatarUrl) {
        UserAccount user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        // 如果修改用户名，检查是否重复
        if (username != null && !username.equals(user.getUsername())) {
            UserAccount existing = userMapper.findByUsername(username);
            if (existing != null && !existing.getId().equals(userId)) {
                return ApiResponse.error(4001, "用户名已存在");
            }
            user.setUsername(username);
        }

        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        userMapper.update(user);

        // 返回更新后的用户信息
        List<String> roles = roleMapper.findRolesByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("roles", roles);

        return ApiResponse.success(result);
    }

    /**
     * 更新密码
     */
    public ApiResponse<String> updatePassword(Long userId, String oldPassword, String newPassword) {
        UserAccount user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }

        // 验证旧密码
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            return ApiResponse.error(401, "原密码错误");
        }

        // 更新密码
        String newPasswordHash = encoder.encode(newPassword);
        userMapper.updatePassword(userId, newPasswordHash);

        return ApiResponse.success("密码修改成功");
    }
}
