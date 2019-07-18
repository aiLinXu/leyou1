package com.leyou.common.auth.entity;

import lombok.Data;

import java.util.Date;

/**
 * @author 虎哥
 */
@Data
public class Payload<T> {
    private String id;
    private T userInfo;
    private Date expiration;
}