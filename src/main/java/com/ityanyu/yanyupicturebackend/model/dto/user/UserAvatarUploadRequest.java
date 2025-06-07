package com.ityanyu.yanyupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/11
 * @Description: 上传图片请求类
 **/
@Data
public class UserAvatarUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
    * 图片id
    * */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;
}
