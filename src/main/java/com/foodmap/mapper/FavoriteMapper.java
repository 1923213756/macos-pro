package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Select("SELECT COUNT(1) FROM favorites WHERE user_id = #{userId} AND shop_id = #{shopId}")
    Integer checkUserFavorited(@Param("userId") Long userId, @Param("shopId") Long shopId);
}