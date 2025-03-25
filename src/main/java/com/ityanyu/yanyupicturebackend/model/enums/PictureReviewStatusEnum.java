package com.ityanyu.yanyupicturebackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * @author: dl
 * @Date: 2025/3/12
 * @Description: 图片审核枚举类
 **/
@Getter
public enum PictureReviewStatusEnum {
    REVIEWING("审核中", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    private final String text;

    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.getValue() == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
