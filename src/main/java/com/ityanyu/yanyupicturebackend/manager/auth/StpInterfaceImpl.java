package com.ityanyu.yanyupicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.ityanyu.yanyupicturebackend.model.entity.Picture;
import com.ityanyu.yanyupicturebackend.model.entity.Space;
import com.ityanyu.yanyupicturebackend.model.entity.SpaceUser;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.enums.SpaceRoleEnum;
import com.ityanyu.yanyupicturebackend.model.enums.SpaceTypeEnum;
import com.ityanyu.yanyupicturebackend.service.PictureService;
import com.ityanyu.yanyupicturebackend.service.SpaceService;
import com.ityanyu.yanyupicturebackend.service.SpaceUserService;
import com.ityanyu.yanyupicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.ityanyu.yanyupicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    //默认为 /api
    @Value("${server.servlet.context-path}")
    private String contextPath;

    public StpInterfaceImpl(SpaceUserAuthManager spaceUserAuthManager) {
        this.spaceUserAuthManager = spaceUserAuthManager;
    }

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //1.校验登录类型：如果 loginType 不是 "space"，直接返回空权限列表。
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        //2.管理员权限处理：如果当前用户为管理员，直接返回管理员权限列表。
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        //3.获取上下文对象：从请求中获取 SpaceUserAuthContext 上下文，检查上下文字段是否为空。
        // 如果上下文中所有字段均为空（如没有空间或图片信息），视为公共图库操作，直接返回管理员权限列表。
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
        //4.校验登录状态：通过 loginId 获取当前登录用户信息。如果用户未登录，抛出未授权异常；
        // 否则获取用户的唯一标识 userId，用于后续权限判断。
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
        //5.从上下文中优先获取 SpaceUser 对象：如果上下文中存在 SpaceUser 对象，直接根据其角色获取权限码列表。
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null){
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        //6.通过 spaceUserId 获取空间用户信息：如果上下文中存在 spaceUserId：
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            //7.查询对应的 SpaceUser 数据。如果未找到，抛出数据未找到异常。
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到空间用户信息");
            }
            //8.校验当前登录用户是否属于该空间，如果不是，返回空权限列表。
            SpaceUser loginSpaceUser  = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            //9.否则，根据登录用户在该空间的角色，返回相应的权限码列表。
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        //通过 spaceId 或 pictureId 获取空间或图片信息
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            //如果 spaceId 不存在：使用 pictureId 查询图片信息，并通过图片的 spaceId 继续判断权限；
            //如果 pictureId 和 spaceId 均为空，默认视为管理员权限。
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            //对于公共图库：如果图片是当前用户上传的，或者当前用户为管理员，返回管理员权限列表；
            //如果图片不是当前用户上传的，返回仅允许查看的权限码。
            if(spaceId == null){
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                }else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        //获取 Space 对象并判断空间类型：查询 Space 信息，如果未找到空间数据，抛出数据未找到异常。
        // 否则根据空间类型进行判断
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        //私有空间：仅空间所有者和管理员有权限（即返回全部权限），其他用户返回空权限列表。
        if (SpaceTypeEnum.PRIVATE.getValue() == space.getSpaceType()) {
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }else {
                return new ArrayList<>();
            }
        }else {
            //团队空间：查询登录用户在该空间的角色，并返回对应的权限码列表。如果用户不属于该空间，返回空权限列表。
             spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, userId)
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .one();
             if (spaceUser == null) {
                 return new ArrayList<>();
             }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 判断所有对象是否为空
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }


    /**
     * 本项目不使用   返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        //获取当前 HTTP 请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        //确定请求内容类型
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        if (ContentType.JSON.getValue().equals(contentType)) {
            //即 POST 请求
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            //即 GET 请求
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjectUtil.isNotNull(id)) {
            String requestURI = request.getRequestURI();
            String partURI = requestURI.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partURI, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }
}
