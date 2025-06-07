package com.ityanyu.yanyupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ityanyu.yanyupicturebackend.common.DeleteRequest;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import com.ityanyu.yanyupicturebackend.manager.sharding.DynamicShardingManager;
import com.ityanyu.yanyupicturebackend.model.dto.space.SpaceAddRequest;
import com.ityanyu.yanyupicturebackend.model.dto.space.SpaceQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.Picture;
import com.ityanyu.yanyupicturebackend.model.entity.Space;
import com.ityanyu.yanyupicturebackend.model.entity.SpaceUser;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.enums.SpaceLevelEnum;
import com.ityanyu.yanyupicturebackend.model.enums.SpaceRoleEnum;
import com.ityanyu.yanyupicturebackend.model.enums.SpaceTypeEnum;
import com.ityanyu.yanyupicturebackend.model.vo.SpaceVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;
import com.ityanyu.yanyupicturebackend.service.PictureService;
import com.ityanyu.yanyupicturebackend.service.SpaceService;
import com.ityanyu.yanyupicturebackend.mapper.SpaceMapper;
import com.ityanyu.yanyupicturebackend.service.SpaceUserService;
import com.ityanyu.yanyupicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author 32845
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-03-25 14:59:51
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    @Lazy
    private PictureService pictureService;

    Map<Long, Object> lockMap = new ConcurrentHashMap<>();

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;
    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //1.填充参数默认值
        //在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //补充空间类型，默认为私有空间
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        //2.校验参数
        validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        //3.校验权限，非管理员只能创建普通级别的空间
        //创建的空间级别不是普通级，并且创建的用户也不是管理员
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        //4.控制同一用户只能创建一个私有空间
        //针对用户加锁 开启事务
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        Long newSpaceId = transactionTemplate.execute(status -> {
            synchronized (lock) {
                try {
                    //userId 指userId字段
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, space.getSpaceType())
                            .exists();
                    if (exists){
                        ThrowUtils.throwIf(SpaceTypeEnum.PRIVATE.getValue() == space.getSpaceType(), ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
                        ThrowUtils.throwIf(SpaceTypeEnum.TEAM.getValue() == space.getSpaceType(), ErrorCode.OPERATION_ERROR, "每个用户仅能创建一个团队空间");
                    }
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                    //如果是团队空间，关联新增团队成员记录，且当且用户的权限是管理员
                    if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()){
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        result = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                    }
//                    //创建分表
//                    dynamicShardingManager.createSpacePictureTable(space);
                    //返回空间id
                    return space.getId();
                } finally {
                    // 防止内存泄漏
                    lockMap.remove(userId);
                }
            }
        });
        //返回结果是包装类，可以做一些处理
        return Optional.ofNullable(newSpaceId).orElse(-1L);
    }

    /**
     * 校验空间数据
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, Boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        //add 为 true 表示是创建图库
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR,
                    "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR,
                    "空间级别不能为空");
            ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR,
                    "空间类型不能为空");
        }
        //更新操作，即要修改空间级别
        ThrowUtils.throwIf(spaceLevel != null && spaceLevelEnum == null, ErrorCode.PARAMS_ERROR,
                "空间级别不存在");
        ThrowUtils.throwIf(spaceName != null && spaceName.length() > 8, ErrorCode.PARAMS_ERROR,
                "空间名称过长");
        ThrowUtils.throwIf(spaceType != null && spaceTypeEnum == null, ErrorCode.PARAMS_ERROR,
                "空间类型不存在");
    }

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        //拼接查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取脱敏后的单个空间信息（单条）
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        //对象装封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = spaceVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取脱敏后的空间信息列表（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //对象列表 -> 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        //获取用户id集合
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());
        //查询用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        //校验
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            //为空再填充，即如果空间本身没有设置限额，才会自动填充，保证了灵活性。
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 空间权限校验
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可访问
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     */
    @Override
    public void deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        //判断是否存在
        Space oldSpace = this.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        //仅本人和管理员可以删除
        this.checkSpaceAuth(loginUser,oldSpace);
        transactionTemplate.execute(status -> {
            //判断是私人空间 还是团队空间
            boolean result;
            //删除空间
            result = this.removeById(oldSpace.getId());
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            if (oldSpace.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                //团队空间 删除 spaceUser
                result = spaceUserService.remove(
                        Wrappers.<SpaceUser>lambdaQuery()
                                .eq(SpaceUser::getSpaceId, oldSpace.getId())
                );
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            }
            // 删除空间所有的图片
            result = pictureService.remove(
                    Wrappers.<Picture>lambdaQuery()
                            .eq(Picture::getSpaceId, oldSpace.getId())
            );
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return null;
        });
        //操作数据库
    }
}




