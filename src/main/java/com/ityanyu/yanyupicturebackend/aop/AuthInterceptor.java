package com.ityanyu.yanyupicturebackend.aop;

import com.ityanyu.yanyupicturebackend.annotation.AuthCheck;
import com.ityanyu.yanyupicturebackend.constant.UserConstant;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.enums.UserRoleEnum;
import com.ityanyu.yanyupicturebackend.service.UserService;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.Throw;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author: dl
 * @Date: 2025/3/4
 * @Description: 使用aop进行权限校验
 **/
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 权限校验 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //1.获取必须具有的权限
        String mustRole = authCheck.mustRole();
        //2.获取当前用户信息
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getUserLogin(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //3.不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        //以下是需要权限
        //获取用户当前的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        //无权限，拒绝
        ThrowUtils.throwIf(userRoleEnum     == null, ErrorCode.NOT_AUTH_ERROR);
        //需要管理员权限，但用户没有管理员权限，拒绝
        ThrowUtils.throwIf(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum),
                ErrorCode.NOT_AUTH_ERROR);
        //通过权限校验
        return joinPoint.proceed();
    }
}
