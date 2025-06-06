package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Select("SELECT COUNT(1) FROM likes WHERE userId = #{userId} AND reviewId = #{reviewId}")
    Integer checkUserLiked(@Param("userId") Long userId, @Param("reviewId") Long reviewId);

    // 删除点赞记录
    int deleteLike(@Param("userId") Long userId, @Param("reviewId") Long reviewId);
}