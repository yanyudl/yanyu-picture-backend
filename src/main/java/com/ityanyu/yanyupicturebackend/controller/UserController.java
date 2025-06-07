package com.ityanyu.yanyupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ityanyu.yanyupicturebackend.annotation.AuthCheck;
import com.ityanyu.yanyupicturebackend.common.BaseResponse;
import com.ityanyu.yanyupicturebackend.common.DeleteRequest;
import com.ityanyu.yanyupicturebackend.common.ResultUtils;
import com.ityanyu.yanyupicturebackend.constant.UserConstant;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import com.ityanyu.yanyupicturebackend.manager.LocalCacheManager;
import com.ityanyu.yanyupicturebackend.manager.email.CheckCodeUtils;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureUploadRequest;
import com.ityanyu.yanyupicturebackend.model.dto.user.*;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.enums.UserEmailCodeTypeEnum;
import com.ityanyu.yanyupicturebackend.model.vo.LoginUserVO;
import com.ityanyu.yanyupicturebackend.model.vo.PictureVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;
import com.ityanyu.yanyupicturebackend.service.UserService;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author: dl
 * @Date: 2025/3/1
 * @Description:
 **/
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final LocalCacheManager LOCAL_CACHE
            = LocalCacheManager.getInstance();

    private static final String KEY_PREFIX = "picture:sendMailCode:";

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String email = userRegisterRequest.getEmail();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String code = userRegisterRequest.getCode();
        //获取缓存 key
        String cleanEmail = email.trim();
        String cacheKey = getCacheKey(cleanEmail, UserEmailCodeTypeEnum.REGISTER.getValue());
        //获取缓存中的验证码
        //先从 caffeine 中取
        String cacheCode = LOCAL_CACHE.get(cacheKey);
        if (cacheCode == null) {
            cacheCode = stringRedisTemplate.opsForValue().get(cacheKey);
        }
        ThrowUtils.throwIf(!code.equals(cacheCode), ErrorCode.PARAMS_ERROR, "验证码错误");
        long result = userService.userRegister(cleanEmail, userPassword, checkPassword);
        stringRedisTemplate.delete(cacheKey);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccountOrEmail = userLoginRequest.getUserAccountOrEmail();
        String userPassword = userLoginRequest.getUserPassword();
        String serververifycode = userLoginRequest.getSerververifycode();
        String verifyCode = userLoginRequest.getVerifyCode();

        String cacheVerifyCode = stringRedisTemplate.opsForValue().get(serververifycode);
        // 无论验证码是否正确 ，直接删除
        stringRedisTemplate.delete(serververifycode);
        if (!verifyCode.equals(cacheVerifyCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccountOrEmail, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getUserLogin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVo(loginUser));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> Logout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.Logout(request);
        return ResultUtils.success(result);
    }

    /**
     * 添加用户（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        //设置默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户（仅管理员  未脱敏）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取用户（脱敏后的）
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户(逻辑删除，仅管理员)
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() < 0
                , ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userEditRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize)
                , userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/getCode")
    public BaseResponse<String> mail(@RequestBody UserEmailCodeRequest userEmailCodeRequest) {
        String email = userEmailCodeRequest.getEmail();
        String type = userEmailCodeRequest.getType();
        ThrowUtils.throwIf(type == null, ErrorCode.PARAMS_ERROR);
        String cleanEmail = email.trim();

        if (!isValidEmail(cleanEmail)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
//            return ResultUtils.success("邮箱格式错误");
        }

        //获取缓存 key
        String cacheKey = getCacheKey(cleanEmail,type);

        // 防止频繁请求
        if (LOCAL_CACHE.get(cacheKey) != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复发送验证码");
//            return ResultUtils.success("请勿重复发送验证码");
        }

        // 生成 6 位验证码
        int authNum = new Random().nextInt(899999) + 100000;
        String authCode = String.valueOf(authNum);

        // 发送邮件
        String message = CheckCodeUtils.getEmailCode(cleanEmail, authCode, type);

        // 存入 Redis (5 分钟)
        stringRedisTemplate.opsForValue().set(cacheKey, authCode, 5, TimeUnit.MINUTES);

        // 存入本地缓存 (1 分钟防刷)
        LOCAL_CACHE.put(cacheKey, authCode);

        return ResultUtils.success(message);
    }

    /**
     * 生成图片验证码
     */
    @GetMapping("/generate")
    public BaseResponse<Map<String, String>> generateCaptcha() {
        // 1. 生成验证码（4位数字）
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER); // 纯数字

        // 2. 生成唯一ID（用于前端提交校验）
        String captchaId = UUID.randomUUID().toString();
        String code = captcha.text().toLowerCase();    // 验证码答案

        // 3. 存入redis缓存
        stringRedisTemplate.opsForValue().set(captchaId, code, 2, TimeUnit.MINUTES);

        // 4. 返回Base64图片 + captchaId
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("base64Captcha", captcha.toBase64());
        return ResultUtils.success(result);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change_password")
    public BaseResponse<Boolean> changePassword(@RequestBody UserChangePasswordRequest userChangePasswordRequest,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(userChangePasswordRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = userChangePasswordRequest.getId();
        String oldPassword = userChangePasswordRequest.getOldPassword();
        String newPassword = userChangePasswordRequest.getNewPassword();
        String checkPassword = userChangePasswordRequest.getCheckPassword();
        //校验权限 是否是当前用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.getId().equals(id), ErrorCode.NO_AUTH_ERROR,
                "非当前用户，无权限修改密码");
        boolean result = userService.changePassword(id, oldPassword, newPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 修改邮箱
     */
    @PostMapping("/change_email")
    public BaseResponse<Boolean> changeEmail(@RequestBody UserChangeEmailRequest userChangeEmailRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(userChangeEmailRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = userChangeEmailRequest.getId();
        String newEmail = userChangeEmailRequest.getNewEmail();
        String code = userChangeEmailRequest.getVerificationCode();
        //校验权限 是否是当前用户
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!loginUser.getId().equals(id), ErrorCode.NO_AUTH_ERROR,
                "非当前用户，无权限修改密码");
        //校验邮箱
        ThrowUtils.throwIf(!isValidEmail(newEmail), ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        //获取缓存key
        String cleanEmail = newEmail.trim();
        String cacheKey = getCacheKey(cleanEmail,UserEmailCodeTypeEnum.CHANGE_EMAIL.getValue());
        String cacheCode = stringRedisTemplate.opsForValue().get(cacheKey);
        ThrowUtils.throwIf(!code.equals(cacheCode), ErrorCode.PARAMS_ERROR, "验证码无效或已过期");
        boolean result = userService.changeEmail(id, cleanEmail);
        stringRedisTemplate.delete(cacheKey);
        return ResultUtils.success(result);
    }

    /**
     * 修改密码
     */
    @PostMapping("/reset_password")
    public BaseResponse<Boolean> resetPassword(@RequestBody UserResetPasswordRequest userResetPasswordRequest) {
        ThrowUtils.throwIf(userResetPasswordRequest == null, ErrorCode.PARAMS_ERROR);
        String email = userResetPasswordRequest.getEmail();
        String newPassword = userResetPasswordRequest.getNewPassword();
        String checkPassword = userResetPasswordRequest.getCheckPassword();
        String code = userResetPasswordRequest.getCode();
        ThrowUtils.throwIf(!isValidEmail(email), ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        //获取缓存key
        String cleanEmail = email.trim();
        String cacheKey = getCacheKey(cleanEmail,UserEmailCodeTypeEnum.RESET_PASSWORD.getValue());
        String cacheCode = stringRedisTemplate.opsForValue().get(cacheKey);
        //校验验证码
        ThrowUtils.throwIf(!code.equals(cacheCode), ErrorCode.PARAMS_ERROR, "验证码无效或已过期");
        boolean result = userService.resetPassword(cleanEmail, newPassword, checkPassword);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"密码修改失败");
        return ResultUtils.success(true);
    }

    /**
     * 用户头像上传
     */
    @PostMapping("/avatar")
    public BaseResponse<String> uploadUserAvatar (@RequestPart("file") MultipartFile multipartFile,
                                                  HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String userAvatarUrl = userService.uploadUserAvatar(multipartFile,loginUser);
        ThrowUtils.throwIf(userAvatarUrl == null, ErrorCode.SYSTEM_ERROR,"头像上传失败，请重新上传");
        return ResultUtils.success(userAvatarUrl);
    }

    /**
     * 校验邮箱格式
     */
    private static boolean isValidEmail(String email) {
        return email.matches("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
    }

    /**
     * 获取缓存 key
     */
    private String getCacheKey(String cleanEmail, String type) {
        String hashKey = DigestUtils.md5DigestAsHex(cleanEmail.getBytes());
        return KEY_PREFIX + type + ":" + hashKey;
    }

}



