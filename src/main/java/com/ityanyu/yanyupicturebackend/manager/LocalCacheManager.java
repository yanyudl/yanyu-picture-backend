package com.ityanyu.yanyupicturebackend.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.concurrent.TimeUnit;

/**
 * @author: dl
 * @Date: 2025/3/19
 * @Description: 本地缓存管理类
 **/
public class LocalCacheManager {
    // 单例模式 - 只初始化一次缓存
    private static final LocalCacheManager INSTANCE = new LocalCacheManager();

    //Caffeine本地缓存
    private final Cache<String,String> localCache;

    // 私有构造函数，防止外部实例化
    private LocalCacheManager() {
        this.localCache = Caffeine.newBuilder()
                .initialCapacity(1024)
                .maximumSize(10000L)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
    }

    //获取缓存实列
    public static LocalCacheManager getInstance() {
        return INSTANCE;
    }

    //获取缓存
    public String get(String key) {
        return localCache.getIfPresent(key);
    }

    //设置缓存
    public void put(String key, String value) {
        localCache.put(key, value);
    }

    //移除缓存
    public void remove(String key) {
        localCache.invalidate(key);
    }

    //清楚所有缓存
    public void clear() {
        localCache.invalidateAll();
    }
}
