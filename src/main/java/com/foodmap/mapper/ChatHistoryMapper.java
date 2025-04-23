package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.pojo.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
    /**
     * 查询指定会话的历史记录（按时间顺序）
     */
    @Select("SELECT * FROM chat_histories WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatHistory> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查询指定会话的最近历史记录（分页）
     */
    @Select("SELECT * FROM chat_histories WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<ChatHistory> findRecentBySessionId(@Param("sessionId") String sessionId, Page<ChatHistory> page);
}