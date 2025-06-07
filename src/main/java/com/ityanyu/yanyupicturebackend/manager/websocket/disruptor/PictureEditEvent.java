package com.ityanyu.yanyupicturebackend.manager.websocket.disruptor;

import com.ityanyu.yanyupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author: dl
 * @Date: 2025/4/10
 * @Description: Disruptor 事件
 **/
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}

