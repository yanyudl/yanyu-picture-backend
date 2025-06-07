package com.ityanyu.yanyupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/6
 * @Description: 空间成员权限类
 **/
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;

}
