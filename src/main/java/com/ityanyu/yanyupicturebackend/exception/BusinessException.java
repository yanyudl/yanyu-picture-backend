package com.ityanyu.yanyupicturebackend.exception;

import lombok.Getter;

/**
 * @author: dl
 * @Date: 2025/3/1
 * @Description: 自定义业务异常
 **/
@Getter
public class BusinessException extends RuntimeException{

    /*
    * 状态码
    * */
    private final int code;

    /**
     *
     * @param code 状态码
     * @param message 错误信息
     */
    public BusinessException(int code ,String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode,String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
