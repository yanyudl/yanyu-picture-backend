package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/3
 * @Description: 用户注册请求
 **/
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -6511143835011149413L;
    /*
    * 账号
    * */
    private String userAccount;

    /*
     * 密码
     * */
    private String userPassword;
}
