package com.adoption.auth.config;

import com.adoption.auth.model.UserAccount;
import com.adoption.auth.repository.UserMapper;
import com.adoption.auth.repository.UserRoleMapper;
import com.adoption.common.constant.RoleEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class AdminInitializer {

    private final UserMapper userMapper;
    private final UserRoleMapper roleMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.phone}")
    private String adminPhone;

    public AdminInitializer(UserMapper userMapper, UserRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @PostConstruct
    public void initAdmin() {
        // 检查是否已有超级管理员
        UserAccount exist = userMapper.findByUsername(adminUsername);
        if (exist == null) {
            UserAccount admin = new UserAccount();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);
            admin.setPhone(adminPhone);
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.setStatus("ACTIVE");

            userMapper.insert(admin);
            roleMapper.insertUserRole(admin.getId(), RoleEnum.ADMIN.name());

            System.out.println("✅ 已初始化超级管理员账号: " + adminUsername);
        } else {
            System.out.println("ℹ️ 超级管理员已存在: " + exist.getUsername());
        }
    }
}
