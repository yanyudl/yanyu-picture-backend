package com.ityanyu.yanyupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.ityanyu.yanyupicturebackend.config.CosClientConfig;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/6
 * @Description:
 **/
@Slf4j
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片且解析图片的基本信息
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        //对图片进行处理
        PicOperations picOperations = new PicOperations();
        //1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        //1.文件压缩格式，转成 webp
        //生成 webp key
        List<PicOperations.Rule> rules = new ArrayList<>();
        String webpKey = FileUtil.mainName(key) + ".webp";
        //设置压缩规则
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        //2.生成缩略图为 .png 格式，方便用于以图搜图
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        String thumbnailKey = FileUtil.mainName(key) + "_thumbnail.png";
        //设置缩略图规则 将缩略图的格式改为 png 支持百度识图
        thumbnailRule.setFileId(thumbnailKey);
        thumbnailRule.setBucket(cosClientConfig.getBucket());
        thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>/format/png", 256, 256));
        rules.add(thumbnailRule);
        //构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 删除对象 公共图库
     *
     * @param key 唯一键
     */
    public void deleteObject(String key, String indexOf) {
        int startIndex = key.indexOf(indexOf);
        String realKey = key.substring(startIndex);
        cosClient.deleteObject(cosClientConfig.getBucket(), realKey);
    }

    /**
     * 获取图片主色调
     *
     * @param key 文件 key
     * @return 图片主色调
     */
    public String getImageAve(String key) {
        GetObjectRequest getObj = new GetObjectRequest(cosClientConfig.getBucket(), key);
        String rule = "imageAve";
        getObj.putCustomQueryParameter(rule, null);
        COSObject object = cosClient.getObject(getObj);
        COSObjectInputStream objectContent = object.getObjectContent();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse httpResponse = httpClient.execute(objectContent.getHttpRequest());
            String response = EntityUtils.toString(httpResponse.getEntity());
            return JSONUtil.parseObj(response).getStr("RGB");
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

}
