package com.foodmap.dto.request;

import lombok.Data;

/**
 * 商铺登录请求参数
 */
@Data
public class ShopLoginDTO {

    private String shopname;

    private String shopPassword;

}