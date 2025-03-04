package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/3
 * @Description: 用户注册请求
 **/
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 7371330871908547417L;

    /*
    * 账号
    * */
    private String userAccount;

    /*
     * 密码
     * */
    private String userPassword;

    /*
     * 确认密码
     * */
    private String checkPassword;

    /*
     * 用户名
     * */
    private String userName;
}
