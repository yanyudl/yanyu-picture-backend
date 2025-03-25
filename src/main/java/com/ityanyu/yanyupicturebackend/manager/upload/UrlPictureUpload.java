package com.ityanyu.yanyupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: dl
 * @Date: 2025/3/13
 * @Description: URL图片上传
 **/
@Service
public class UrlPictureUpload extends PictureUploadTemplate{
    private String contentType;

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        //1.是否为空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        //2.校验 URL 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件格式不正确");
        }
        //3.校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");
        //4. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try{
            response = HttpUtil.createRequest(Method.HEAD,fileUrl).execute();
            //未正常返回，无需其他校验
            if (response.getStatus() != HttpStatus.HTTP_OK){
                return;
            }
            //5. 校验文件类型
            contentType = response.header("Content-Type");
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

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 去掉查询参数
        String cleanUrl = fileUrl.split("\\?")[0];
        if (!FileUtil.extName(cleanUrl).isEmpty()) {
            // 如果已经有后缀，直接返回
            return cleanUrl;
        }
        //如果没有后缀
        ThrowUtils.throwIf(StrUtil.isBlank(contentType), ErrorCode.PARAMS_ERROR,"文件类型错误");
        String extension = getFileExtensionFromContentType(contentType);
        ThrowUtils.throwIf(StrUtil.isBlank(extension), ErrorCode.PARAMS_ERROR);
        return cleanUrl + "." + extension;
    }

    private String getFileExtensionFromContentType(String contentType) {
        Map<String, String> contentTypeToExtension = new HashMap<>();
        contentTypeToExtension.put("image/jpeg", "jpg");
        contentTypeToExtension.put("image/jpg", "jpg");
        contentTypeToExtension.put("image/png", "png");
        contentTypeToExtension.put("image/gif", "gif");
        contentTypeToExtension.put("image/webp", "webp");

        return contentTypeToExtension.getOrDefault(contentType.toLowerCase(), "");
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }


}

