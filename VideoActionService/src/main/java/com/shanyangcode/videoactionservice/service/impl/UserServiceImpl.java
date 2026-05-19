package com.shanyangcode.videoactionservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.videoactionservice.mapper.UserMapper;
import com.shanyangcode.videoactionservice.model.entity.User;
import com.shanyangcode.videoactionservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
