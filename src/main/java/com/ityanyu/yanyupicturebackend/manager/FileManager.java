package com.ityanyu.yanyupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.ityanyu.yanyupicturebackend.config.CosClientConfig;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import com.ityanyu.yanyupicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/11
 * @Description: @deprecated 已废弃，改为使用 upload 包的模板方法优化  文件服务
 **/
@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     *
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,
                                              String uploadPathPrefix ) {
        //1.校验参数
        validPicture(multipartFile);
        //2.图片上传地址 进行拼接
        //生成一个16位的uuid
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //创建临时文件
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath,file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            //计算宽高比
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round((double) picWidth /picHeight,2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            //删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     * @return
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR,"文件不能为空");
        //1.校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 5 * ONE_MB,ErrorCode.PARAMS_ERROR,"文件大小不能超过5MB");
        //检查文件上传的后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR,"文件类型错误");
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

    /**
     * 通过url上传图片
     *
     * @param fileUrl 文件地址
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl,
                                             String uploadPathPrefix ) {
        //1.todo：校验参数
        validPicture(fileUrl);
        //2.图片上传地址 进行拼接
        //生成一个16位的uuid
        String uuid = RandomUtil.randomString(16);
        //todo:
//        String originalFilename = multipartFile.getOriginalFilename();
        String originalFilename = FileUtil.mainName(fileUrl);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        //创建临时文件
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
//            todo
//            multipartFile.transferTo(file);
            HttpUtil.downloadFile(fileUrl,file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath,file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            //计算宽高比
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round((double) picWidth /picHeight,2).doubleValue();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        }finally {
            //删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * url校验
     *
     * @param fileUrl
     * @return
     */
    private void validPicture(String fileUrl) {
        //1.是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl),ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        //2.校验 URL 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件格式不正确");
        }
        //3.校验 URL 协议
        ThrowUtils.throwIf(!fileUrl.startsWith("https://") || !fileUrl.startsWith("http://"),
                ErrorCode.PARAMS_ERROR,"仅支持 HTTP 或 HTTPS 协议的文件地址");
        //4. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try{
            response = HttpUtil.createRequest(Method.HEAD,fileUrl).execute();
            //未正常返回，无需其他校验
            if (response.getStatus() != HttpStatus.HTTP_OK){
                return;
            }
            //5. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)){
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/gif", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //6. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)){
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long FIVE_MB = 5 * 1024 * 1024L; // 限制文件大小为 5MB
                    ThrowUtils.throwIf(contentLength > FIVE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        }finally {
            if (response != null){
                response.close();
            }
        }
    }
}
