package com.ityanyu.yanyupicturebackend.manager.websocket.model;

import com.ityanyu.yanyupicturebackend.model.vo.PictureVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: dl
 * @Date: 2025/4/8
 * @Description: 图片编辑响应消息类
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 图片信息
     */
    private PictureVO pictureVO;
}
