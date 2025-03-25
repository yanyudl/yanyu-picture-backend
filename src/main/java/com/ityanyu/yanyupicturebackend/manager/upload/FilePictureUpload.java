package com.ityanyu.yanyupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/13
 * @Description: 文件图片上传
 **/
@Service
public class FilePictureUpload extends PictureUploadTemplate{
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
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

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);}
}
