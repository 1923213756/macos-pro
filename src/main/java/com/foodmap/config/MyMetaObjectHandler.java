package com.foodmap.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 创建时间和更新时间自动填充
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        // 为默认状态赋值
        this.strictInsertFill(metaObject, "status", String.class, "ACTIVE");

        // 为点赞数赋默认值
        this.strictInsertFill(metaObject, "likeCount", Integer.class, 0);

        // 为新增的评分字段设置默认值，防止空值
        if (getFieldValByName("environmentRating", metaObject) == null) {
            this.strictInsertFill(metaObject, "environmentRating", Integer.class, 0);
        }
        if (getFieldValByName("serviceRating", metaObject) == null) {
            this.strictInsertFill(metaObject, "serviceRating", Integer.class, 0);
        }
        if (getFieldValByName("tasteRating", metaObject) == null) {
            this.strictInsertFill(metaObject, "tasteRating", Integer.class, 0);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}