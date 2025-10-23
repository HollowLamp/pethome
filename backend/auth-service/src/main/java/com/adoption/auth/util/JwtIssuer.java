package com.adoption.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.List;

public class JwtIssuer {
    private final Key key;
    private final long expiration;

    public JwtIssuer(String secret, long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

    /**
     * 生成 JWT
     * @param userId 用户ID
     * @param roles 用户角色列表
     * @return token 字符串
     */
    public String generateToken(Long userId, List<String> roles) {
        return Jwts.builder()
                .setSubject(userId.toString()) // 用户ID
                .claim("roles", String.join(",", roles)) // 用户角色
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
