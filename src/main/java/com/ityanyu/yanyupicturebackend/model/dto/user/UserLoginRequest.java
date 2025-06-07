package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/3
 * @Description: 用户登录请求类
 **/
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -6511143835011149413L;
    /*
    * 账号或邮箱
    * */
    private String userAccountOrEmail;

    /*
     * 密码
     * */
    private String userPassword;

    /**
     * redis缓存 Id
     */
    private String serververifycode;

    /**
     * 图形验证码
     */
    private String verifyCode;
}
