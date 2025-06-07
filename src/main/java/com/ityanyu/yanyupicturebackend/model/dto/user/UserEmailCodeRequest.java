package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/14
 * @Description: 邮箱验证码请求类
 **/
@Data
public class UserEmailCodeRequest implements Serializable {
    /**
     * 邮箱
     */
    private String email;

    /**
     * 使用类型（如：注册，找回密码，更改绑定邮箱）
     */
    private String type;

    private static final long serialVersionUID = -6511143835011149413L;

}
