package com.ityanyu.yanyupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ityanyu.yanyupicturebackend.model.dto.user.UserAvatarUploadRequest;
import com.ityanyu.yanyupicturebackend.model.dto.user.UserQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ityanyu.yanyupicturebackend.model.vo.LoginUserVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

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
     * @param email   邮箱
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return 用户ID
     */
    long userRegister(String email, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccountOrEmail  账户或邮箱
     * @param userPassword 密码
     * @return 返回脱敏后的用户数据
     */
    LoginUserVO userLogin(String userAccountOrEmail, String userPassword, HttpServletRequest request);

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
    User getLoginUser(HttpServletRequest request);

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

    /**
     * 判断用户是否是管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 修改密码（主动修改）
     *
     * @param id
     * @param oldPassword
     * @param newPassword
     * @param checkPassword
     * @return
     */
    boolean changePassword(Long id, String oldPassword, String newPassword, String checkPassword);

    /**
     * 修改邮箱
     *
     * @param id
     * @param email
     * @return
     */
    boolean changeEmail(Long id, String email);

    /**
     * 重置密码（邮箱验证码重置）
     *
     * @param email
     * @param newPassword
     * @param checkPassword
     * @return
     */
    boolean resetPassword(String email, String newPassword, String checkPassword);

    /**
     * 用户头像上传
     *
     * @param multipartFile
     * @param loginUser
     * @return
     */
    String uploadUserAvatar(MultipartFile multipartFile,  User loginUser);
}
