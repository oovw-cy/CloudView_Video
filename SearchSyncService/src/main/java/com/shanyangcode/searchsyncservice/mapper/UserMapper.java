package com.shanyangcode.searchsyncservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.shanyangcode.searchsyncservice.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
