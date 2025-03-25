package com.ityanyu.yanyupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/11
 * @Description: 上传图片请求类
 **/
@Data
public class PictureUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
    * 图片id
    * */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

}
