package com.ityanyu.yanyupicturebackend.model.enums;

import lombok.Getter;

/**
 * @author: dl
 * @Date: 2025/3/3
 * @Description: 用户角色枚举类
 **/
@Getter
public enum UserRoleEnum {
    ADMIN("管理员","admin"),
    USER("普通用户","user");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (UserRoleEnum userRoleEnum: UserRoleEnum.values()){
            if(userRoleEnum.getValue().equals(value)){
                return userRoleEnum;
            }
        }
        return null;
    }
}
