package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/15
 * @Description: 重置密码请求类
 **/
@Data
public class UserResetPasswordRequest implements Serializable {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 验证码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}
