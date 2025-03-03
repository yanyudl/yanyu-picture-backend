package com.ityanyu.yanyupicturebackend.controller;

import com.ityanyu.yanyupicturebackend.common.BaseResponse;
import com.ityanyu.yanyupicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: dl
 * @Date: 2025/3/1
 * @Description: 健康测试
 **/
@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     *
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("hello");
    }
}
