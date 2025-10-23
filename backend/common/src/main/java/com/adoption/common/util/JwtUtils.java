package com.adoption.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

public class JwtUtils {

    /**
     * 解析 JWT，返回 Claims
     */
    public static Claims parseToken(String token, String secret) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取用户 ID
     */
    public static String getUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * 获取用户角色
     */
    public static String getRoles(Claims claims) {
        return claims.get("roles", String.class);
    }
}
