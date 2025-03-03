package com.ityanyu.yanyupicturebackend.common;

import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author: dl
 * @Date: 2025/3/1
 * @Description: 全局响应封装类
 **/
@Data
public class BaseResponse<T> implements Serializable {
    /*
    * 状态码
    * */
    private int code;

    /*
    * 返回的数据
    * */
    private T data;

    /*
    * 额外的信息
    * */
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
}
