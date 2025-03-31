package com.ityanyu.yanyupicturebackend.api.imageSearch.model;

import lombok.Data;

/**
 * @author: dl
 * @Date: 2025/3/29
 * @Description: 以图搜图结果类
 **/
@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;
}
