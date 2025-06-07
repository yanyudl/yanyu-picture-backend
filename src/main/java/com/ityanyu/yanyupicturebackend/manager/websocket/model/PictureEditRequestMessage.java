package com.ityanyu.yanyupicturebackend.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: dl
 * @Date: 2025/4/8
 * @Description: 图片编辑请求消息类
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作（如：放大、缩小、左旋、右旋）
     */
    private String editAction;
}
