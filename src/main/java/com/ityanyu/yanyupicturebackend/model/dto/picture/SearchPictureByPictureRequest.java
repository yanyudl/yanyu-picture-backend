package com.ityanyu.yanyupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/30
 * @Description: 以图搜图请求类
 **/
@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
