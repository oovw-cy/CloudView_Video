package com.shanyangcode.videoactionservice.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.IntegerCodec;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CounterUtil {

    @Resource
    private RedissonClient redissonClient;
    

    /**
     * 增加并返回计数
     *
     * @param key                     缓存键
     * @param timeInterval            时间间隔
     * @param timeUnit                时间间隔单位
     * @param expirationTimeInSeconds 计数器缓存过期时间
     * @return long
     */
    public long incrAndGetCounter(String key, int timeInterval, TimeUnit timeUnit, long expirationTimeInSeconds) {
        if (StrUtil.isBlank(key)) {
            return 0;
        }

        // 根据时间粒度生成 Redis Key
        long timeFactor;
        switch (timeUnit) {
            case SECONDS:
                timeFactor = Instant.now().getEpochSecond() / timeInterval;
                break;
            case MINUTES:
                timeFactor = Instant.now().getEpochSecond() / (timeInterval * 60L);
                break;
            case HOURS:
                timeFactor = Instant.now().getEpochSecond() / (timeInterval * 3600L);
                break;
            default:
                throw new IllegalArgumentException("不支持的单位");
        }

        String redisKey = key + ":" + timeFactor;

        // Lua 脚本
        String luaScript =
                "if redis.call('exists', KEYS[1]) == 1 then " +  // 1. 判断key是否存在
                        "  return redis.call('incr', KEYS[1]); " +      // 2. 存在：自增+1
                        "else " +
                        "  redis.call('set', KEYS[1], 1); " +           // 3. 不存在：设为1
                        "  redis.call('expire', KEYS[1], ARGV[1]); " +  // 4. 设置过期时间
                        "  return 1; " +                                // 5. 返回1
                        "end";

        // 获取脚本执行对象
        RScript script = redissonClient.getScript(IntegerCodec.INSTANCE);
        // 执行脚本
        Object countObj = script.eval(
                RScript.Mode.READ_WRITE,    // 读写模式
                luaScript,                  // 我们写的Lua脚本
                RScript.ReturnType.INTEGER, // 返回整数
                Collections.singletonList(redisKey), // 传入Redis Key
                expirationTimeInSeconds     // 传入过期时间
        );
// 转换并返回计数结果
        return (long) countObj;
    }
}