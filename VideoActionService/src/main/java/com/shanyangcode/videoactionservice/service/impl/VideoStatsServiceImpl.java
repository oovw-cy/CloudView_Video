package com.shanyangcode.videoactionservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.videoactionservice.mapper.VideoStatsMapper;
import com.shanyangcode.videoactionservice.model.entity.VideoStats;
import com.shanyangcode.videoactionservice.service.VideoStatsService;
import org.springframework.stereotype.Service;

/**
* @author 717
* @description 针对表【video_stats(视频数据统计表)】的数据库操作Service实现
* @createDate 2026-05-07 09:28:33
*/
@Service
public class VideoStatsServiceImpl extends ServiceImpl<VideoStatsMapper, VideoStats>
    implements VideoStatsService {

}




