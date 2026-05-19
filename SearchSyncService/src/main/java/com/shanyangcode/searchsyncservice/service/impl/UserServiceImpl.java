package com.shanyangcode.searchsyncservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.searchsyncservice.mapper.UserMapper;
import com.shanyangcode.searchsyncservice.model.entity.User;
import com.shanyangcode.searchsyncservice.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
