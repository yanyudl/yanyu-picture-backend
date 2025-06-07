package com.ityanyu.yanyupicturebackend.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: dl
 * @Date: 2025/4/5
 * @Description: 空间成员角色枚举
 **/
@Getter
public enum SpaceRoleEnum {

    VIEWER("浏览者","viewer"),
    EDITOR("编辑者","editor"),
    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum: SpaceRoleEnum.values()) {
            if (spaceRoleEnum.getValue().equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return 文本列表
     */
    public static List<String> getAllText(){
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return 值列表
     */
    public static List<String> getAllValue(){
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
