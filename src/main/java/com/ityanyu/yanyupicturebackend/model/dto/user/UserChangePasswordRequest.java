package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/15
 * @Description: 修改密码请求类
 **/
@Data
public class UserChangePasswordRequest implements Serializable {

    /**
     * 用户 Id
     */
    private Long id;

    /**
     * 旧密码
     * */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
