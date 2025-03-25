package com.ityanyu.yanyupicturebackend.controller;

import com.ityanyu.yanyupicturebackend.annotation.AuthCheck;
import com.ityanyu.yanyupicturebackend.common.BaseResponse;
import com.ityanyu.yanyupicturebackend.common.ResultUtils;
import com.ityanyu.yanyupicturebackend.constant.UserConstant;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author: dl
 * @Date: 2025/3/6
 * @Description:
 **/
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        //创建临时文件
        File file = null;
        //上传文件
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath,file);
            //返回文件路径
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("upload file error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }finally {
            //删除临时文件
            if (file != null) {
                boolean delete = file.delete();
                if (!delete){
                    log.error("delete file error, filepath = " + filepath);
                }
            }
        }
    }

    @PostMapping("/test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObject cosObject = null;
        try {
            //获取要下载的流
            cosObject = cosManager.getObject(filepath);
            //获取实际存储的内容
            COSObjectInputStream objectContent = cosObject.getObjectContent();
            //处理
            byte[] byteArray = IOUtils.toByteArray(objectContent);
            //设置响应头
            response.setContentType("application/octet-stream;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filepath);
            //写入响应中
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("download file error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }finally {
            if (cosObject != null) {
                cosObject.close();
            }
        }

    }
}
