package com.shanyangcode.videoactionservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SnowflakeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.videoactionservice.mapper.LikeMapper;
import com.shanyangcode.videoactionservice.model.dto.CancelVideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.entity.Like;
import com.shanyangcode.videoactionservice.model.entity.User;
import com.shanyangcode.videoactionservice.model.entity.Video;
import com.shanyangcode.videoactionservice.model.entity.VideoStats;
import com.shanyangcode.videoactionservice.service.LikeService;
import com.shanyangcode.videoactionservice.service.UserService;
import com.shanyangcode.videoactionservice.service.VideoService;
import com.shanyangcode.videoactionservice.service.VideoStatsService;
import com.shanyangcode.videoactionservice.utils.CounterUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
* @author 717
* @description 针对表【like(点赞表)】的数据库操作Service实现
* @createDate 2026-05-07 16:30:37
*/
@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Resource
    private UserService userService;
    @Resource
    private VideoService videoService;
    @Resource
    private VideoStatsService videoStatsService;
    @Resource
    private CounterUtil counterUtil;

    @Override
    @Transactional
    public Long likeVideo(VideoActionRequest videoActionRequest) {
         //检测点赞频率是否过快
         crawlerLikeDetect(videoActionRequest);

        // 校验判断视频是否存在
        ThrowUtils.throwIf(!videoService.lambdaQuery().eq(Video::getVideoId, videoActionRequest.getVideoId()).exists(), ErrorCode.VIDEO_NOT_FOUND_ERROR);

        // 校验判断用户是否存在
        ThrowUtils.throwIf(!userService.lambdaQuery().eq(User::getUserId, videoActionRequest.getUserId()).exists(), ErrorCode.USER_NOT_EXISTS);

        // 查询是否已经点赞
        ThrowUtils.throwIf(this.lambdaQuery().eq(Like::getVideoId, videoActionRequest.getVideoId()).eq(Like::getUserId, videoActionRequest.getUserId()).exists(), ErrorCode.VIDEO_LIKED_ERROR);

        // 保存点赞记录
        Like likeVideo = new Like();
        likeVideo.setVideoId(videoActionRequest.getVideoId());
        likeVideo.setUserId(videoActionRequest.getUserId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowflakeConstant.WORKER_ID, SnowflakeConstant.DATA_CENTER_ID);
        likeVideo.setLikeId(snowflake.nextId());
        boolean save = this.save(likeVideo);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);

        // 视频点赞数+1
        boolean updated = videoStatsService.lambdaUpdate().setSql("like_count = like_count + 1").eq(VideoStats::getVideoId, likeVideo.getVideoId()).update();
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新视频统计失败");

        return likeVideo.getLikeId();

    }
    private void crawlerLikeDetect(VideoActionRequest videoActionRequest) {
        // 调用多少次时告警
        final int WARN_COUNT = 2;
        // 拼接访问 key
        String key = String.format("like:%s:%s", videoActionRequest.getUserId(), videoActionRequest.getVideoId());
        // 统计一分钟内访问次数，80 秒过期
        long count = counterUtil.incrAndGetCounter(key, 1, TimeUnit.MINUTES, 80);
        // 是否告警
        ThrowUtils.throwIf(count > WARN_COUNT, ErrorCode.ACCESS_TOO_FREQUENTLY);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelLikeVideo(CancelVideoActionRequest cancelVideoActionRequest) {
        // 查询是否存在
        ThrowUtils.throwIf(!this.lambdaQuery().eq(Like::getLikeId, cancelVideoActionRequest.getId()).exists(), ErrorCode.VIDEO_LIKED_NOT_EXISTS);

        // 删除点赞记录
        boolean remove = this.removeById(cancelVideoActionRequest.getId());
        ThrowUtils.throwIf(!remove, ErrorCode.SYSTEM_ERROR);

        // 视频点赞数 -1
        boolean updated = videoStatsService.lambdaUpdate().setSql("like_count = like_count - 1").eq(VideoStats::getVideoId, cancelVideoActionRequest.getVideoId()).update();
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新视频统计失败");

        return true;
    }
}




