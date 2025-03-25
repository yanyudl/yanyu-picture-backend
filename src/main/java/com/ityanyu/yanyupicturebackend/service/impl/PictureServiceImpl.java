package com.ityanyu.yanyupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ityanyu.yanyupicturebackend.exception.BusinessException;
import com.ityanyu.yanyupicturebackend.exception.ErrorCode;
import com.ityanyu.yanyupicturebackend.exception.ThrowUtils;
import com.ityanyu.yanyupicturebackend.manager.CosManager;
import com.ityanyu.yanyupicturebackend.manager.CountManager;
import com.ityanyu.yanyupicturebackend.manager.LocalCacheManager;
import com.ityanyu.yanyupicturebackend.manager.upload.FilePictureUpload;
import com.ityanyu.yanyupicturebackend.manager.upload.PictureUploadTemplate;
import com.ityanyu.yanyupicturebackend.manager.upload.UrlPictureUpload;
import com.ityanyu.yanyupicturebackend.model.dto.file.UploadPictureResult;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureQueryRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureReviewRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.ityanyu.yanyupicturebackend.model.dto.picture.PictureUploadRequest;
import com.ityanyu.yanyupicturebackend.model.entity.Picture;
import com.ityanyu.yanyupicturebackend.model.entity.User;
import com.ityanyu.yanyupicturebackend.model.enums.PictureReviewStatusEnum;
import com.ityanyu.yanyupicturebackend.model.vo.PictureVO;
import com.ityanyu.yanyupicturebackend.model.vo.UserVO;
import com.ityanyu.yanyupicturebackend.service.PictureService;
import com.ityanyu.yanyupicturebackend.mapper.PictureMapper;
import com.ityanyu.yanyupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 32845
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-03-06 18:10:12
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CountManager countManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final long MAX_HOT = 10;

    static String key;

    //冷数据存储时间 2-5 分钟
    private static final int TIME = 120 + RandomUtil.randomInt(0, 180);

    private static final LocalCacheManager LOCAL_CACHE
            = LocalCacheManager.getInstance();

    private static final String KEY_PREFIX = "yanyuPicture:picture:";

    @Resource
    private UserService userService;
    @Autowired
    private CosManager cosManager;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_AUTH_ERROR);
        //判断是新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
            //只用管理员和本人能编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NOT_AUTH_ERROR);
            }
        }
        //上传图片，公共图库以public为目录，再以创建用户id为目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        //构造图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //无论是创建还是更新图片，都需要将审核状态设为未审核,设置图片审核状态
        fillPictureStatus(picture, loginUser);
        //如果 pictureId 不为空表示更新 要补充id
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());

        }
        boolean result = this.saveOrUpdate(picture);
        //TODO 添加删除对象存储逻辑
//        if (picture.getId() != null){
//            //不为空表示更新
//            this.clearPictureFile(picture);
//        }
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        //TODO: 保证原子性
        //删除 redis 和 caffeine 中的缓存
        LOCAL_CACHE.remove(key);
        stringRedisTemplate.delete(key);
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        //拼接查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like("name", searchText).or().like("introduction", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(ObjUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        //标签查询 JSON字符串
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取脱敏后的单个图片信息
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象装封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //对象列表 -> 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //获取用户id集合
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        //查询用户信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //填充
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVO(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片数据
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 1024, ErrorCode.PARAMS_ERROR, "introduction过长");
        }
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        //图片id不存在 || 没有审核状态 || 已经审核过的图片不能在改为待审核
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否存在图片
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验审核状态是重复
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus)
                , ErrorCode.PARAMS_ERROR, "请勿重复审核");
        //更新审核状态
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 设置图片审核状态
     *
     * @param picture
     * @param loginUser
     * @return
     */
    @Override
    public void fillPictureStatus(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            //设置管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        Integer offset = pictureUploadByBatchRequest.getOffset();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");

        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 构建抓取 URL，添加偏移量参数
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&first=%s&count=%s&mmasync=1",
                searchText, offset, count);

        Document document;
        try {
            // 使用 Jsoup 连接并获取页面内容
            document = Jsoup.connect(fetchUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36")
                    .get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        // 提取图片元素
//        Elements imgElementList = div.select("img.mimg");
        Elements imgElementList = div.select(".iusc");

        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            // 如果已经达到抓取数量上限，退出循环
            if (uploadCount >= count) {
                break;
            }

//            String fileUrl = imgElement.attr("src");
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }

            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        //删除 redis 和 caffeine 中的缓存
        LOCAL_CACHE.remove(key);
        stringRedisTemplate.delete(key);
        return uploadCount;
    }

    /**
     * 分页获取图片列表 进行缓存
     *
     * @param pictureQueryRequest page and size是固定的
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        //校验数据
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        //构建 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        key = KEY_PREFIX + hashKey;
        //从本地缓存中取，caffeine
        String cacheValue = LOCAL_CACHE.get(key);
        if (cacheValue != null) {
            //如果本地缓存 caffeine命中，直接返回
            return JSONUtil.toBean(cacheValue, Page.class);
        }
        //caffeine 未命中，查询 redis
        String redisValue = stringRedisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            //如果 redis 命中,并加入 caffeine
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(redisValue, Page.class);
            LOCAL_CACHE.put(key, JSONUtil.toJsonStr(pictureVOPage));
            return pictureVOPage;
        }
        //redis 也未命中，从数据库中获取
        int current = pictureQueryRequest.getCurrent();
        int size = pictureQueryRequest.getPageSize();
        QueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryRequest);
        queryWrapper.select("id", "url", "thumbnailUrl", "name", "tags", "category");
        Page<Picture> picturePage = page(new Page<>(current, size),
                queryWrapper);
        //脱敏
        Page<PictureVO> pictureVOPage = getPictureVOPage(picturePage, request);
        //存入缓存 caffeine 和 redis 中
        //为redis设置一个过期时间 5-10分钟
        LOCAL_CACHE.put(key, JSONUtil.toJsonStr(pictureVOPage));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(pictureVOPage), TIME, TimeUnit.SECONDS);
        return pictureVOPage;
    }

    /**
     * 根据 id 获取图片（脱敏）
     */
    @Override
    public Picture getPictureVOById(Long id) {
        //检验参数
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        //先查询 caffeine
        String key = KEY_PREFIX + id;
        String cacheValue = LOCAL_CACHE.get(key);
        if (cacheValue != null) {
            //如果本地缓存 caffeine命中，直接返回
            return JSONUtil.toBean(cacheValue, Picture.class);
        }
        //未命中，查询 redis，并且开始统计次数
        //统计计数器返回的次数
        long count = countManager.incrAndGetCounter("hot:image" + id);
        String redisValue = stringRedisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            //如果 redis 命中，直接返回 并且如果访问次数大于 hot_key 就加入caffeine
            Picture picture = JSONUtil.toBean(redisValue, Picture.class);
            if (count >= MAX_HOT){
                LOCAL_CACHE.put(key, JSONUtil.toJsonStr(picture));

            }
            return picture;
        }
        //未命中，查询数据库并加入redis
        Picture pictureById = getById(id);
        //防止缓存击穿
        if (pictureById == null) {
            stringRedisTemplate.opsForValue().set(key,"", 2, TimeUnit.SECONDS);
        }
        //时间为 5-10 分钟，防止同一时间内大量key消失，导致缓存雪崩
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(pictureById), TIME, TimeUnit.SECONDS);
        return pictureById;
    }

    /*
     * 删除图片文件
     * */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        //判断该图片是否被多条记录只用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //又不止一条记录使用过该url
        if (count > 1) {
            //不清理
            return;
        }
        //清理
        cosManager.deleteObject(oldPicture.getUrl());
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }
}






