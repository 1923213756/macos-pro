package com.foodmap.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheUtil {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheUtil.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.info("缓存设置成功: {}", key);
        } catch (Exception e) {
            log.error("缓存设置失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 设置缓存（默认2小时过期）
     */
    public void set(String key, Object value) {
        this.set(key, value, 2, TimeUnit.HOURS);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.info("缓存命中: {}", key);
            } else {
                log.info("缓存未命中: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("获取缓存失败: {}, 错误: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.info("缓存删除成功: {}", key);
        } catch (Exception e) {
            log.error("缓存删除失败: {}, 错误: {}", key, e.getMessage());
        }
    }

    /**
     * 删除匹配的缓存
     */
    public void deleteByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("批量删除缓存成功, 模式: {}, 删除数量: {}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("批量删除缓存失败, 模式: {}, 错误: {}", pattern, e.getMessage());
        }
    }
}