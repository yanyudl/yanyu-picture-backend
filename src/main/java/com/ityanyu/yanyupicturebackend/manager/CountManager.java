package com.ityanyu.yanyupicturebackend.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: dl
 * @Date: 2025/3/18
 * @Description: 使用 Redis Hash 结构优化计数器，减少 Key 爆炸
 */
@Service
public class CountManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 增加并返回计数，默认统计一分钟内的计数结果
     *
     * @param key 缓存键
     * @return 计数值
     */
    public long incrAndGetCounter(String key) {
        return incrAndGetCounter(key, 1, TimeUnit.MINUTES);
    }

    /**
     * 增加并返回计数
     *
     * @param key          缓存键
     * @param timeInterval 时间间隔
     * @param timeUnit     时间间隔单位
     * @return 计数值
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit) {
        int expirationTimeInSeconds = (int) timeUnit.toSeconds(timeInterval * 5L);
        return incrAndGetCounter(key, timeInterval, timeUnit, expirationTimeInSeconds);
    }

    /**
     * 增加并返回计数
     *
     * @param key                     缓存键
     * @param timeInterval            时间间隔
     * @param timeUnit                时间间隔单位
     * @param expirationTimeInSeconds 计数器缓存过期时间
     * @return 计数值
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit, long expirationTimeInSeconds) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        // 计算时间片（timeFactor）
        long timeFactor;
        switch (timeUnit) {
            case SECONDS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval;
                break;
            case MINUTES:
                timeFactor = Instant.now().getEpochSecond() / (timeInterval * 60);
                break;
            case HOURS:
                timeFactor = Instant.now().getEpochSecond() / (timeInterval * 3600);
                break;
            default:
                throw new IllegalArgumentException("不支持的时间单位");
        }

        // Redis Key & Hash Field
        String redisKey = "counter:" + key; // 主要 key
        String hashField = String.valueOf(timeFactor); // Hash Field 作为时间分片

        // Lua 脚本：使用 HINCRBY 计数 & 设置过期时间
        String luaScript =
                "local exists = redis.call('EXISTS', KEYS[1], ARGV[1]); " +
                        "local count = redis.call('HINCRBY', KEYS[1], ARGV[1], 1); " +
                        "if exists == 0 then " +
                        "  redis.call('EXPIRE', KEYS[1], ARGV[2]); " + // 只在新增 field 时设置过期时间
                        "end; " +
                        "return count;";

        // 创建 Redis 脚本
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new StaticScriptSource(luaScript));
        redisScript.setResultType(Long.class);

        List<String> keys = Collections.singletonList(redisKey);
        List<String> args = List.of(hashField, String.valueOf(expirationTimeInSeconds));

        // 执行 Lua 脚本
        Long count = stringRedisTemplate.execute(redisScript, keys, args.toArray());
        return count != null ? count : 0;
    }
}
