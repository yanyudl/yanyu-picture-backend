package com.ityanyu.yanyupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/13
 * @Description: 抓取图片请求类
 **/
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = -1557151624743113975L;
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

    /**
     * 偏移量 （防止抓取相同图片）
     */
    private Integer offset = 0;

}

