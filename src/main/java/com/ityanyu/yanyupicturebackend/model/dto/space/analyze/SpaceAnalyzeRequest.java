package com.ityanyu.yanyupicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/2
 * @Description: 通用 空间分析请求类
 **/
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}
