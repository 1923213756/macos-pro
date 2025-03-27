package com.foodmap.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 通用响应结果封装类
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    //判断成功
    private Boolean success;

    //错误信息
    private String errorMsg;

    //响应内容
    private T data;

    //带结果返回成功
    public static <T> Result<T> success(T data) {
        return new Result<>(true, null, data);
    }

    //返回无结果成功
    public static <T> Result<T> success() {
        return new Result<>(true, null, null);
    }

    //返回失败结果
    public static <T> Result<T> fail(String errorMsg) {
        return new Result<>(false, errorMsg, null);
    }

    //自定义
    public static <T> Result<T> build(Boolean success, String errorMsg, T data) {
        return new Result<>(success, errorMsg, data);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return success != null && success;
    }
}