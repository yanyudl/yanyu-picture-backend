package com.ityanyu.yanyupicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/4/2
 * @Description: 空间使用排行分析请求类
 **/
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
