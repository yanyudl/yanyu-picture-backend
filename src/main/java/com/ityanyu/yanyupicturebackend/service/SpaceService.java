package com.ityanyu.yanyupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ityanyu.yanyupicturebackend.common.DeleteRequest;
import com.ityanyu.yanyupicturebackend.model.dto.space.SpaceAddRequest;
import com.ityanyu.yanyupicturebackend.model.dto.space.SpaceQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 32845
 * @description 针对表【space(空间)】的数据库操作 Service
 * @createDate 2025-03-25 14:59:51
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间数据
     *
     * @param space
     * @param add
     */
    void validSpace(Space space, Boolean add);

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取脱敏后的单个空间信息（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取脱敏后的空间信息列表（分页）
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> picturePage, HttpServletRequest request);

    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 空间权限校验
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     */
    void deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request);
}