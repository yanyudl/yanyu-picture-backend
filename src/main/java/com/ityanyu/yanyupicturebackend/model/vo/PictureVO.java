package com.ityanyu.yanyupicturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ityanyu.yanyupicturebackend.model.entity.Picture;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/11
 * @Description:
 **/
@Data
public class PictureVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /*
    * 创建用户信息
    * */
    private UserVO userVO;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * vo转实体类
     */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) return null;
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVO,picture);
        //tags标签类型不同，需要手动转换
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    /**
     * 实体类转vo
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) return null;
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture,pictureVO);
        //tags标签类型不同，需要手动转换
        pictureVO.setTags(JSONUtil.toList(picture.getTags(),String.class));
        return pictureVO;
    }
}
