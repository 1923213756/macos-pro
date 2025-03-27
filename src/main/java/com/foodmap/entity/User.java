package com.foodmap.entity;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String password;  // 加密后的密码
    private Integer userType; // 0-普通用户, 1-商家
    private Integer phone;


}