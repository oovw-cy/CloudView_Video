package com.shanyangcode.videoactionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.videoactionservice.mapper.BulletMapper;
import com.shanyangcode.videoactionservice.model.dto.SendBulletRequest;
import com.shanyangcode.videoactionservice.model.entity.Bullet;
import com.shanyangcode.videoactionservice.model.entity.User;
import com.shanyangcode.videoactionservice.model.vo.OnlineBulletResponse;
import com.shanyangcode.videoactionservice.service.BulletService;
import com.shanyangcode.videoactionservice.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
* @author 717
* @description 针对表【bullet(弹幕表)】的数据库操作Service实现
* @createDate 2026-05-06 13:34:29
*/
@Service
public class BulletServiceImpl extends ServiceImpl<BulletMapper, Bullet> implements BulletService {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveBulletToMySQL(SendBulletRequest sendBulletRequest) {
        Long videoId = sendBulletRequest.getVideoId();
        Long userId = sendBulletRequest.getUserId();

        // 校验视频是否存在（优化为 exists 查询）
        // ThrowUtils.throwIf(!videoService.lambdaQuery().eq(Video::getVideoId, videoId).exists(), ErrorCode.VIDEO_NOT_FOUND_ERROR);

        // 校验用户是否存在User
        ThrowUtils.throwIf(!userService.lambdaQuery().eq(User::getUserId, userId).exists(), ErrorCode.USER_NOT_EXISTS);

        // 使用原子操作更新 VideoStats
        // boolean updated = videoStatsService.lambdaUpdate().setSql("bullet_count = bullet_count + 1").eq(VideoStats::getVideoId, videoId).update();
        // ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新视频统计失败");

        // 保存弹幕
        Bullet bullet = new Bullet();
        bullet.setVideoId(videoId);
        bullet.setUserId(userId);
        bullet.setContent(sendBulletRequest.getContent());
        bullet.setPlaybackTime(sendBulletRequest.getPlaybackTime());
        bullet.setBulletId(sendBulletRequest.getBulletId());
        boolean saved = this.save(bullet);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "保存弹幕失败");
    }

    @Override
    public List<OnlineBulletResponse> getBulletList(Long videoId) {
        List<OnlineBulletResponse> onlineBulletResponses = new ArrayList<>();
        String cacheKey = "video:" + videoId + ":bullet";
        if (stringRedisTemplate.hasKey(cacheKey)) {
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().rangeWithScores(cacheKey, 0, -1);
            System.out.println(tuples);
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String[] parts = tuple.getValue().split(":");
                OnlineBulletResponse onlineBulletResponse = new OnlineBulletResponse();
                onlineBulletResponse.setUserId(parts[0]);
                onlineBulletResponse.setBulletId(parts[1]);
                onlineBulletResponse.setText(parts[2]);
                onlineBulletResponse.setPlaybackTime(tuple.getScore());
                onlineBulletResponses.add(onlineBulletResponse);
            }
        } else {
            LambdaQueryWrapper<Bullet> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Bullet::getVideoId, videoId);
            List<Bullet> bullets = this.list(lambdaQueryWrapper);
            if (bullets.isEmpty()) {
                return onlineBulletResponses;
            }
            Set<ZSetOperations.TypedTuple<String>> addTuples = new HashSet<>();
            for (Bullet bullet : bullets) {
                String bulletId = bullet.getBulletId().toString();
                String userId = bullet.getUserId().toString();
                String content = bullet.getContent();
                Double playbackTime = bullet.getPlaybackTime();
                OnlineBulletResponse onlineBulletResponse = new OnlineBulletResponse();
                onlineBulletResponse.setText(content);
                onlineBulletResponse.setPlaybackTime(playbackTime);
                onlineBulletResponse.setBulletId(bulletId);
                onlineBulletResponse.setUserId(userId);
                onlineBulletResponses.add(onlineBulletResponse);
                addTuples.add(new DefaultTypedTuple<>(userId + ":" + bulletId + ":" + content, playbackTime));

            }
            try {
                stringRedisTemplate.opsForZSet().add(cacheKey, addTuples);
                // 随机设置过期时间，防止缓存雪崩
                stringRedisTemplate.expire(cacheKey, 72 * 3600 + ThreadLocalRandom.current().nextInt(3600), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Redis 保存弹幕失败");
            }
        }
        // 对弹幕按时间排序
        onlineBulletResponses.sort(Comparator.comparingDouble(OnlineBulletResponse::getPlaybackTime));
        return onlineBulletResponses;
    }

    @Override
    public boolean bulletExists(Long bulletId) {
        return this.lambdaQuery().eq(Bullet::getBulletId, bulletId).exists();
    }

}




