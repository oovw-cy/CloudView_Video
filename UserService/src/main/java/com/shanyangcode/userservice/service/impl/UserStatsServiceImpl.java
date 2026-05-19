package com.shanyangcode.userservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.userservice.mapper.UserStatsMapper;
import com.shanyangcode.userservice.model.entity.UserStats;
import com.shanyangcode.userservice.service.UserStatsService;
import org.springframework.stereotype.Service;

@Service
public class UserStatsServiceImpl extends ServiceImpl<UserStatsMapper, UserStats> implements UserStatsService {
}
