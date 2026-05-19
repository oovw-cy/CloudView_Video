package com.shanyangcode.videoactionservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.videoactionservice.mapper.UserStatsMapper;
import com.shanyangcode.videoactionservice.model.entity.UserStats;
import com.shanyangcode.videoactionservice.service.UserStatsService;
import org.springframework.stereotype.Service;

@Service
public class UserStatsServiceImpl extends ServiceImpl<UserStatsMapper, UserStats> implements UserStatsService {
}
