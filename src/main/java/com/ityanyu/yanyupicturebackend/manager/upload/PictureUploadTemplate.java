package com.ityanyu.yanyupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.ityanyu.yanyupicturebackend.config.CosClientConfig;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.manager.CosManager;
import com.ityanyu.yanyupicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/11
 * @Description: 使用模板方法模式 对代码进行优化。
 **/
@Service
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片，模板方法，定义上传流程
     *
     * @param inputSource      文件输入源
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource,
                                             String uploadPathPrefix) {
        //1.校验参数
        validPicture(inputSource);
        //2.图片上传地址 进行拼接
        //生成一个16位的uuid
        String uuid = RandomUtil.randomString(16);

        String originFilename = getOriginFilename(inputSource);
        System.out.println("originFilename = " + originFilename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            //3.创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);
            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            String imageAve = cosManager.getImageAve(uploadPath);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //取到修改后的结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                //取到压缩图
                CIObject compressdCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressdCiObject;
                if (objectList.size() > 1) {
                    //取到缩略图
                    thumbnailCiObject = objectList.get(1);
                }
                //封装压缩图返回结果
                return buildResult(originFilename,compressdCiObject,thumbnailCiObject,imageAve);
            }
            // 5. 封装返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo,imageAve);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //6.删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果类
     *
     * @param originFilename
     * @param compressdCiObject
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressdCiObject,
                                            CIObject thumbnailCiObject,String imageAve) {
        //封装返回结果
        //计算宽高比
        int picWidth = compressdCiObject.getWidth();
        int picHeight = compressdCiObject.getHeight();
        double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressdCiObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicSize(compressdCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressdCiObject.getFormat());
        uploadPictureResult.setPicColor(imageAve);
        return uploadPictureResult;
    }
    /**
     * 封装返回结果类
     *
     * @param originFilename
     * @param file
     * @param uploadPath
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo, String imageAve) {
        //封装返回结果
        //计算宽高比
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageAve);
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        //删除临时文件
        boolean delete = file.delete();
        if (!delete) {
            log.error("delete file error, filepath = " + file.getAbsolutePath());
        }
    }


}
