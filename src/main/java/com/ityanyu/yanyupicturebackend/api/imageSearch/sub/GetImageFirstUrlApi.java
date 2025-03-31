package com.ityanyu.yanyupicturebackend.api.imageSearch.sub;

import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: dl
 * @Date: 2025/3/30
 * @Description: 获取图片列表接口的 api （step2）
 **/
public class GetImageFirstUrlApi {

    private static final Logger log = LoggerFactory.getLogger(GetImageFirstUrlApi.class);

    /**
     * 获取图片列表地址
     *
     * @param url
     * @return
     */
    public static String getImageFirstUrl(String url) {
        try {
            //使用jsoup获取 HTML内容
            Document document = Jsoup.connect(url)
                    .timeout(5000)
                    .get();

            //获取所有 <script>标签
            Elements scriptElements = document.getElementsByTag("script");

            //遍历 找到包含 firstUrl的那一列
            //"title": "相似图片",
            //"firstUrl": "https:\/\/graph.baidu.com\/ajax\/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=9469011255950737574&sign=1212c63b5e26f4deda09601743327854&tk=8715a&tpl_from=pc",
            for (Element script : scriptElements) {
                String scriptContent  = script.html();
                // \" 是转义字符 即 "  \" 同理
                if (scriptContent.contains("\"firstUrl\"")){
                    //正则表达式提取 firstUrl 的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptContent);
                    if (matcher.find()){
                        String firstUrl = matcher.group(1);
                        //处理转义字符
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到 url");
        } catch (Exception e) {
            log.error("搜索失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    //测试
    public static void main(String[] args) {
        // 请求目标 URL
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=2425562967212869472&sign=1216763b5e26f4deda09601743328791&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println("imageFirstUrl = " + imageFirstUrl);
    }
}
