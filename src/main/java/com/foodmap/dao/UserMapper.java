package com.foodmap.dao;


import com.foodmap.entity.User;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper {
    User selectByUsername(String username);
    int insertUser(User user);
    int countByUsername(String username);
    int countByPhone(Integer phone);
    User selectByPhone(String phone);
}