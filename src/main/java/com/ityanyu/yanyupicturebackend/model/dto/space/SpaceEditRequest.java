package com.ityanyu.yanyupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/25
 * @Description: 空间编辑请求类，仅用于用户
 **/
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}
