package com.ityanyu.yanyupicturebackend.api.imageSearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ityanyu.yanyupicturebackend.api.imageSearch.model.ImageSearchResult;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/30
 * @Description: 获取图片列表（step3）
 **/
@Slf4j
public class GetImageListApi {

    /**
     * 获取图片列表
     *
     * @param url
     * @return
     */
    public static List<ImageSearchResult> getImageList(String url) {
        try {
            //发起 get请求
            HttpResponse response = HttpUtil.createGet(url).execute();

            //获取响应内容
            int statusCode = response.getStatus();
            String body = response.body();

            //处理响应
            if (statusCode == 200) {
                //解析 json 数据并处理
                return processResponse(body);
            }else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
        }catch (Exception e) {
            log.error("获取图片列表失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
    }

    /**
     * 处理接口响应内容
     *
     * @param responseBody
     * @return
     */
    private static List<ImageSearchResult> processResponse(String responseBody) {
        //解析响应对象
        JSONObject jsonObject = new JSONObject(responseBody);
        if (!jsonObject.containsKey("data")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        JSONObject data = jsonObject.getJSONObject("data");
        if (!data.containsKey("list")){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        }
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }

    //测试
    public static void main(String[] args) {
        String url = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=2425562967212869472&sign=1216763b5e26f4deda09601743328791&tk=f6edc&tpl_from=pc";
        List<ImageSearchResult> imageList = getImageList(url);
        System.out.println("imageList = " + imageList);
    }
}
