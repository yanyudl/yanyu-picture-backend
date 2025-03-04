package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/4
 * @Description: 用户更新请求类（管理员）
 **/
@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 957312503838737606L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

}
