package com.ityanyu.yanyupicturebackend.model.enums;

import lombok.Getter;

/**
 * @author: dl
 * @Date: 2025/4/14
 * @Description: 邮箱验证码使用类型枚举
 **/
@Getter
public enum UserEmailCodeTypeEnum {

    REGISTER("用户注册", "register"),
    RESET_PASSWORD("找回密码", "reset_password"),
    CHANGE_EMAIL("更改绑定邮箱", "change_email");

    private final String text;
    private final String value;

    UserEmailCodeTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据code获取枚举
     *
     * @param value
     * @return
     */
    public static UserEmailCodeTypeEnum getByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (UserEmailCodeTypeEnum userEmailCodeTypeEnum: UserEmailCodeTypeEnum.values()) {
            if (userEmailCodeTypeEnum.getValue().equals(value)) {
                return userEmailCodeTypeEnum;
            }
        }
        return null;
    }

}
