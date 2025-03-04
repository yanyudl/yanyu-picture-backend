package com.ityanyu.yanyupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ityanyu.yanyupicturebackend.model.dto.user.UserQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ityanyu.yanyupicturebackend.model.vo.LoginUserVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 32845
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-03-03 19:35:03
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   账户
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @param userName      用户名
     * @return 用户ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String userName);

    /**
     * 用户登录
     *
     * @param userAccount  账户
     * @param userPassword 密码
     * @return 返回脱敏后的用户数据
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 加密
     *
     * @param password 密码
     * @return 加密密码
     */
    String getEncryptPassword(String password);

    /**
     * 获取脱敏后的登录用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVo(User user);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取当前用户信息
     *
     * @param request 请求
     * @return 用户信息
     */
    User getUserLogin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     */
    boolean Logout(HttpServletRequest request);

    /**
     *获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
