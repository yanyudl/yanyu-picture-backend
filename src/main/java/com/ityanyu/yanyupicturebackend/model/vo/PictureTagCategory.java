package com.ityanyu.yanyupicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/11
 * @Description:
 **/
@Data
public class PictureTagCategory {
    /*
    * 标签列表
    * */
    private List<String> tagList;

    /*
     * 分类列表
     * */
    private List<String> categoryList;

}
