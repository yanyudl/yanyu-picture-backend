package com.ityanyu.yanyupicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: dl
 * @Date: 2025/3/4
 * @Description: 用户权限校验
 **/
//作用域（作用在方法上）
@Target(ElementType.METHOD)
//自定义注解的生命周期
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /*
    * 用户必须具有的权限
    * */
    String mustRole() default "";
}
