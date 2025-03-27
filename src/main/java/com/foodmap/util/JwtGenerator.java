//package com.example.util;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import javax.crypto.SecretKey;
//import java.security.SecureRandom;
//import java.util.Base64;
//import java.util.Date;
//
//public class JwtGenerator {
//    // 安全生成密钥（符合RFC规范）
//    private static String generateSecretKey() {
//        SecureRandom random = new SecureRandom();
//        byte[] keyBytes = new byte[64]; // HS512要求64字节（512位）
//        random.nextBytes(keyBytes);
//        return Base64.getEncoder().encodeToString(keyBytes);
//    }
//
//    // 生成JWT令牌
//    public static String createJwt() {
//        // 步骤1：生成密钥
//        String secretString = generateSecretKey();
//        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretString));
//
//        // 步骤2：构建Payload声明
//        return Jwts.builder()
//                .setSubject("user123")                // 用户唯一标识[6](@ref)
//                .claim("role", "admin")               // 自定义声明
//                .setIssuedAt(new Date())              // 签发时间[1](@ref)
//                .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // 1小时有效期[5](@ref)
//                .signWith(key, SignatureAlgorithm.HS512) // 安全签名[4](@ref)
//                .compact();
//    }
//    //测试密钥生成
//    public static void main(String[] args) {
//        String jwt = createJwt();
//        System.out.println("Generated Secret: " + generateSecretKey());
//        System.out.println("Generated JWT: " + jwt);
//    }
//}