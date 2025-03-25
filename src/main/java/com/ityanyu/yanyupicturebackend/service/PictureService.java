package com.ityanyu.yanyupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ityanyu.yanyupicturebackend.common.BaseResponse;
import com.ityanyu.yanyupicturebackend.common.PageRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureQueryRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureReviewRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureUploadRequest;
import com.ityanyu.yanyupicturebackend.model.dto.user.UserQueryRequest;
import com.ityanyu.yanyupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 32845
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-03-06 18:10:12
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource 文件输入源
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     *获取查询条件
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取脱敏后的单个图片信息
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取脱敏后的图片信息列表
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片数据
     *
     * @param picture
     * @return
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 设置图片审核状态
     *
     * @param picture
     * @param loginUser
     * @return
     */
    void fillPictureStatus(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                              User loginUser);


    /**
     * 分页获取图片列表
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 根据 id 获取图片（脱敏）
     */
    Picture getPictureVOById(Long id);

    /*
    * 删除图片文件
    * */
    void clearPictureFile(Picture oldPicture);
}

