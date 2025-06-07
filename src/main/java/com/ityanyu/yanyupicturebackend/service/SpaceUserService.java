package com.ityanyu.yanyupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ityanyu.yanyupicturebackend.model.dto.space.SpaceAddRequest;
import com.ityanyu.yanyupicturebackend.model.dto.space.SpaceQueryRequest;
import com.ityanyu.yanyupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.ityanyu.yanyupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.Space;
import com.ityanyu.yanyupicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.vo.SpaceUserVO;
import com.ityanyu.yanyupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 32845
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-04-05 21:00:43
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUSer(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员数据
     *
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, Boolean add);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取脱敏后的单个空间成员信息（单条）
     *
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取脱敏后的空间成员信息列表（列表）
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

}
