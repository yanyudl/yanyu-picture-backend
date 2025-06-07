package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/15
 * @Description: 修改邮箱请求类
 **/
@Data
public class UserChangeEmailRequest implements Serializable {

    /**
     * 用户 Id
     */
    private Long id;

    /**
     * 新邮箱
     */
    private String newEmail;

    /**
     * 验证码
     */
    private String verificationCode;

    private static final long serialVersionUID = 1L;
}
