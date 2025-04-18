package com.foodmap.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.User;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(String username);
    int insertUser(User user);
    int countByUsername(String username);
    int countByPhone(String phone);
    User selectByPhone(String phone);
}