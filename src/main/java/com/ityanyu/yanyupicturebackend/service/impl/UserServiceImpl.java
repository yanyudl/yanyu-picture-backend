package com.ityanyu.yanyupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ityanyu.yanyupicturebackend.common.ResultUtils;
import com.ityanyu.yanyupicturebackend.config.CosClientConfig;
import com.ityanyu.yanyupicturebackend.constant.UserConstant;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import com.ityanyu.yanyupicturebackend.manager.CosManager;
import com.ityanyu.yanyupicturebackend.manager.auth.StpKit;
import com.ityanyu.yanyupicturebackend.manager.upload.FilePictureUpload;
import com.ityanyu.yanyupicturebackend.manager.upload.PictureUploadTemplate;
import com.ityanyu.yanyupicturebackend.model.dto.user.UserAvatarUploadRequest;
import com.ityanyu.yanyupicturebackend.model.dto.user.UserQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.enums.UserRoleEnum;
import com.ityanyu.yanyupicturebackend.model.vo.LoginUserVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;
import com.ityanyu.yanyupicturebackend.service.UserService;
import com.ityanyu.yanyupicturebackend.mapper.UserMapper;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 32845
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-03-03 19:35:03
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    // 账号规则：4-20位，仅允许字母、数字、下划线，不能全是数字
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{4,20}$";

    // 邮箱校验
    private static final String EMAIL_REGEX =
            "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

    /**
     * 用户注册
     * ]
     *
     * @param email         邮箱
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return 用户ID
     */
    @Override
    public long userRegister(String email, String userPassword, String checkPassword) {
        //1.校验数据
        ThrowUtils.throwIf(StrUtil.hasBlank(email, userPassword, checkPassword),
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "用户密码过短");
        //2.校验格式 和 密码
        ThrowUtils.throwIf(!Pattern.matches(EMAIL_REGEX, email),
                ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),
                ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        //3.查询数据库，检查账户否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        long count = this.baseMapper.selectCount(queryWrapper);

        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号已经注册");
        //4.密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //5.写入数据库
        User user = new User();
        //获取账号
        int atIndex = email.indexOf("@");
        String userAccount = email.substring(0, atIndex);
        user.setUserAccount(userAccount);
        user.setEmail(email);
        user.setUserPassword(encryptPassword);
        String randomLetters = RandomUtil.randomString(6);
        String userName = String.format("用户_%s", randomLetters);
        user.setUserName(userName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccountOrEmail 账户或邮箱
     * @param userPassword       密码
     * @return 返回脱敏后的用户数据
     */
    @Override
    public LoginUserVO userLogin(String userAccountOrEmail, String userPassword, HttpServletRequest request) {
        //1.校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccountOrEmail, userPassword),
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(userPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "用户密码过短");
        //2.对用户传递的密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询数据库用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userPassword", encryptPassword)
                .and(wrapper -> wrapper.eq("userAccount", userAccountOrEmail)
                        .or()
                        .eq("email", userAccountOrEmail));
        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
        //4.保存用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        //记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVo(user);
    }

    /**
     * 加密
     *
     * @param password 密码
     * @return 加密密码
     */
    @Override
    public String getEncryptPassword(String password) {
        //盐值，混淆密码
        final String SALT = "yanyu";
        return DigestUtils.md5DigestAsHex((password + SALT).getBytes());
    }

    /**
     * 获取脱敏后的登录用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVo(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取当前用户信息
     *
     * @param request 请求
     * @return 用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) attribute;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null,
                ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        //从数据库中查询用户的信息
        currentUser = this.getById(currentUser.getId());
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return currentUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean Logout(HttpServletRequest request) {
        //1.判断用户是否已经登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(attribute == null,
                ErrorCode.OPERATION_ERROR, "用户未登录");
        //2.移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 判断用户是否是管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 修改密码（主动修改）
     *
     * @param id
     * @param oldPassword
     * @param newPassword
     * @param checkPassword
     * @return
     */
    @Override
    public boolean changePassword(Long id, String oldPassword, String newPassword, String checkPassword) {
        //校验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(StrUtil.hasBlank(oldPassword, newPassword, checkPassword),
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(oldPassword.length() < 8 || newPassword.length() < 8 || checkPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "用户密码过短");

        ThrowUtils.throwIf(!newPassword.equals(checkPassword),
                ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        //加密 旧密码
        String oldEncryptPassword = getEncryptPassword(oldPassword);
        //查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("userPassword", oldEncryptPassword);
        User oldUser = this.getOne(queryWrapper);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.PARAMS_ERROR, "旧密码错误");
        // 新增校验：新旧密码不能相同
        ThrowUtils.throwIf(oldPassword.equals(newPassword),
                ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        // 加密 新密码
        String encryptPassword = getEncryptPassword(newPassword);
        User user = new User();
        user.setId(id);
        user.setUserPassword(encryptPassword);
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "修改密码失败");
        return true;
    }

    /**
     * 修改邮箱
     *
     * @param id
     * @param email
     * @return
     */
    @Override
    public boolean changeEmail(Long id, String email) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(StrUtil.hasBlank(email), ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(!Pattern.matches(EMAIL_REGEX, email),
                ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        //查询数据库，看该邮箱是否绑定
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被其他账号绑定");
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "邮箱修改失败");
        return true;
    }

    /**
     * 重置密码（邮箱验证码重置）
     *
     * @param email
     * @param newPassword
     * @param checkPassword
     * @return
     */
    @Override
    public boolean resetPassword(String email, String newPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(email,newPassword,checkPassword),
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(newPassword.length() < 8 || checkPassword.length() <8,
                ErrorCode.PARAMS_ERROR,"密码长度不能小于8位");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword),
                ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        // 验证邮箱格式
        ThrowUtils.throwIf(!Pattern.matches(EMAIL_REGEX,email)
                ,ErrorCode.PARAMS_ERROR,"邮箱格式不正确");
        //查询数据库
        User user = this.lambdaQuery()
                .eq(User::getEmail, email)
                .one();
        ThrowUtils.throwIf(user == null,ErrorCode.NOT_FOUND_ERROR,"该邮箱未注册");
        //加密
        String encryptPassword = getEncryptPassword(newPassword);
        // 更新用户密码
        user.setUserPassword(encryptPassword);
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"密码重置失败，请稍后重试");
        return true;
    }

    /**
     * 用户头像上传
     *
     * @param multipartFile
     * @param loginUser
     * @return
     */
    @Override
    public String uploadUserAvatar(MultipartFile multipartFile,  User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        validAvatar(multipartFile);
        String uploadPathPrefix = "userAvatar";
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //创建临时文件
        File file = null;
        //上传文件
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(uploadPath, file);
            String avatarUrl = cosClientConfig.getHost() + "/" + uploadPath;
            return avatarUrl;
        } catch (Exception e) {
            log.error("upload file error, filepath = " + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }finally {
            //删除临时文件
            if (file != null) {
                boolean delete = file.delete();
                if (!delete){
                    log.error("delete file error, uploadPath = " + uploadPath);
                }
            }
        }
    }

    private void validAvatar(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        //1.校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_MB,ErrorCode.PARAMS_ERROR,"文件大小不能超过2MB");
        //检查文件上传的后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR,"文件类型错误");
    }
}




