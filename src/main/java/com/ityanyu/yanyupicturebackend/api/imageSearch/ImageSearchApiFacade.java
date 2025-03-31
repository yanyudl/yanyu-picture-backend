package com.ityanyu.yanyupicturebackend.api.imageSearch;

import com.ityanyu.yanyupicturebackend.api.imageSearch.model.ImageSearchResult;
import com.ityanyu.yanyupicturebackend.api.imageSearch.sub.GetImageFirstUrlApi;
import com.ityanyu.yanyupicturebackend.api.imageSearch.sub.GetImageListApi;
import com.ityanyu.yanyupicturebackend.api.imageSearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author: dl
 * @Date: 2025/3/30
 * @Description: 图片搜索服务
 **/
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 图片搜索
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
            List<ImageSearchResult> imageSearchResults = searchImage("https://yanyu-picture-1328109360.cos.ap-chengdu.myqcloud.com/space/1905235556707647489/2025-03-30_9IhS9N2RRSNEIoFS_thumbnail.png");
        int i = 0;
        for (ImageSearchResult imageSearchResult : imageSearchResults) {
            i++;
            System.out.println("i = " + i);
            System.out.println("imageSearchResult = " + imageSearchResult);
        }
    }
}
