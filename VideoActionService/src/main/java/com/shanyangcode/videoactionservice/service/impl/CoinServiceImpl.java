package com.shanyangcode.videoactionservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SnowflakeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.videoactionservice.mapper.CoinMapper;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.entity.*;
import com.shanyangcode.videoactionservice.service.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author 717
* @description 针对表【coin(投币表)】的数据库操作Service实现
* @createDate 2026-05-07 16:30:37
*/
@Service
public class CoinServiceImpl extends ServiceImpl<CoinMapper, Coin> implements CoinService {

    @Resource
    private UserService userService;
    @Resource
    private UserStatsService userStatsService;
    @Resource
    private VideoService videoService;
    @Resource
    private VideoStatsService videoStatsService;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean coinVideo(VideoActionRequest videoActionRequest) {

        // 校验判断用户是否已经投币
        ThrowUtils.throwIf(this.lambdaQuery().eq(Coin::getVideoId, videoActionRequest.getVideoId()).eq(Coin::getUserId, videoActionRequest.getUserId()).exists(), ErrorCode.VIDEO_COIN_ERROR);

        // 校验判断视频是否存在
        ThrowUtils.throwIf(!videoService.lambdaQuery().eq(Video::getVideoId, videoActionRequest.getVideoId()).exists(), ErrorCode.VIDEO_NOT_FOUND_ERROR);

        // 校验判断用户是否存在
        User user = userService.lambdaQuery().eq(User::getUserId, videoActionRequest.getUserId()).one();
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXISTS);

        // 获取用户详细信息
        UserStats userStats = userStatsService.getById(user.getUserId());

        // 校验判断用户硬币是否足够
        ThrowUtils.throwIf(userStats.getCoinCount() < 1, ErrorCode.USER_COIN_ERROR);

        // 保存投币记录
        Coin coin = new Coin();
        coin.setVideoId(videoActionRequest.getVideoId());
        coin.setUserId(videoActionRequest.getUserId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowflakeConstant.WORKER_ID, SnowflakeConstant.DATA_CENTER_ID);
        coin.setCoinId(snowflake.nextId());
        boolean save = this.save(coin);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);


        // 视频投币数 +1
        boolean updatedVideoStats = videoStatsService.lambdaUpdate().setSql("coin_count = coin_count + 1").eq(VideoStats::getVideoId, videoActionRequest.getVideoId()).update();
        ThrowUtils.throwIf(!updatedVideoStats, ErrorCode.SYSTEM_ERROR, "更新视频统计失败");


        // 用户硬币数 -1
        boolean updatedUserCoin = userStatsService.lambdaUpdate().setSql("coin_count = coin_count - 1").eq(UserStats::getUserId, videoActionRequest.getUserId()).update();
        ThrowUtils.throwIf(!updatedUserCoin, ErrorCode.SYSTEM_ERROR, "更新用户硬币数失败");

        return true;

    }
}




