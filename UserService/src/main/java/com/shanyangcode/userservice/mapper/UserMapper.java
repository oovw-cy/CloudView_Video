package com.shanyangcode.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.vo.LoginResponse;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    LoginResponse getUserInfo(Long userId);
}
