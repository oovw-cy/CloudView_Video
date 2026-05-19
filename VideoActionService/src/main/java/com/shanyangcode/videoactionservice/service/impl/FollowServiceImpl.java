package com.shanyangcode.videoactionservice.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.SnowflakeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.videoactionservice.mapper.FollowMapper;
import com.shanyangcode.videoactionservice.model.dto.FollowRequest;
import com.shanyangcode.videoactionservice.model.entity.Follow;
import com.shanyangcode.videoactionservice.model.entity.User;
import com.shanyangcode.videoactionservice.model.entity.UserStats;
import com.shanyangcode.videoactionservice.model.vo.UserListResponse;
import com.shanyangcode.videoactionservice.service.FollowService;
import com.shanyangcode.videoactionservice.service.UserService;
import com.shanyangcode.videoactionservice.service.UserStatsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 717
* @description 针对表【follow(关注表)】的数据库操作Service实现
* @createDate 2026-05-07 22:36:28
*/
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Resource
    private UserService userService;
    @Resource
    private UserStatsService userStatsService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean follow(FollowRequest followRequest) {

        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", followRequest.getUserId(), followRequest.getCreatorId());//in操作：查询 user_id 在这两个值里面的用户
        List<User> users = userService.list(queryWrapper);
        ThrowUtils.throwIf(users.size() != 2, ErrorCode.USER_NOT_EXISTS);

        // 查询是否已经关注
        Follow follow = new Follow();
        follow.setUserId(followRequest.getUserId());
        follow.setCreatorId(followRequest.getCreatorId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowflakeConstant.WORKER_ID, SnowflakeConstant.DATA_CENTER_ID);
        follow.setFollowId(snowflake.nextId());
        boolean saved = this.save(follow);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "关注失败");

        // 更新粉丝统计
        boolean updatedFollowers = userStatsService.lambdaUpdate().setSql("followers = followers + 1").eq(UserStats::getUserId, followRequest.getCreatorId()).update();
        ThrowUtils.throwIf(!updatedFollowers, ErrorCode.SYSTEM_ERROR, "更新博主粉丝统计失败");

        // 更新关注统计
        boolean updatedFollowing = userStatsService.lambdaUpdate().setSql("following = following + 1").eq(UserStats::getUserId, followRequest.getUserId()).update();
        ThrowUtils.throwIf(!updatedFollowing, ErrorCode.SYSTEM_ERROR, "更新用户关注统计失败");

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean chanelFollow(FollowRequest followRequest) {

        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", followRequest.getUserId(), followRequest.getCreatorId());
        List<User> users = userService.list(queryWrapper);
        ThrowUtils.throwIf(users.size() != 2, ErrorCode.USER_NOT_EXISTS);

        QueryWrapper<Follow> queryFollowWrapper = new QueryWrapper<>();
        queryFollowWrapper
                .eq("user_id", followRequest.getUserId())
                .eq("creator_id", followRequest.getCreatorId());

        Follow follow = this.getOne(queryFollowWrapper);
        ThrowUtils.throwIf(follow == null, ErrorCode.OPERATION_ERROR, "未关注该用户，不能取消关注");

        // 删除关注记录
        boolean removed = this.remove(queryFollowWrapper);
        ThrowUtils.throwIf(!removed, ErrorCode.SYSTEM_ERROR, "取消关注失败");

        // 更新博主粉丝统计
        boolean updatedFollowers = userStatsService.lambdaUpdate()
                .setSql("followers = followers - 1")
                .eq(UserStats::getUserId, followRequest.getCreatorId())
                .update();
        ThrowUtils.throwIf(!updatedFollowers, ErrorCode.SYSTEM_ERROR, "更新博主粉丝统计失败");

        // 更新用户关注统计
        boolean updatedFollowing = userStatsService.lambdaUpdate()
                .setSql("following = following - 1")
                .eq(UserStats::getUserId, followRequest.getUserId())
                .update();
        ThrowUtils.throwIf(!updatedFollowing, ErrorCode.SYSTEM_ERROR, "更新用户关注统计失败");


        return true;
    }

    @Override
    public List<UserListResponse> followList(Long userId) {
        // 查询用户是否存在关注
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<Follow> followList = this.list(queryWrapper);
        if (followList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询【所有被关注的UP主的用户信息】
        // 1. 从关注记录中，提取所有 被关注的UP主ID（CreatorId）
        // 2. stream流映射 → 转成Set集合（去重）
        // 3. listByIds：批量根据用户ID查询用户表，一次性查出所有UP主信息
        List<User> userList = userService.listByIds(
                followList.stream().map(Follow::getCreatorId).collect(Collectors.toSet())
        );
        // 将用户列表转为Map，方便快速查找
        // key = 用户ID(UP主ID)，value = User对象
        // 作用：根据ID快速查用户，不用循环遍历，速度极快（O(1)查找）
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
        List<UserListResponse> userListResponses = new ArrayList<>();
        // 遍历所有关注记录
        for (Follow follow : followList) {
            UserListResponse userListResponse = new UserListResponse();
            userListResponse.setUserId(follow.getCreatorId());
            userListResponse.setAvatar(userMap.get(follow.getCreatorId()).getAvatar());
            userListResponse.setNickname(userMap.get(follow.getCreatorId()).getNickname());
            userListResponse.setDescription(userMap.get(follow.getCreatorId()).getDescription());
            // 添加到结果列表
            userListResponses.add(userListResponse);
        }
        return userListResponses;
    }

    @Override
    public List<UserListResponse> followerList(Long userId) {
        // 查询用户是否存在粉丝
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creator_id", userId);
        List<Follow> followerList = this.list(queryWrapper);
        if (followerList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询【所有粉丝的用户信息】
        // 1. 从关注记录中，提取所有 被关注的UP主ID（CreatorId）
        // 2. stream流映射 → 转成Set集合（去重）
        // 3. listByIds：批量根据用户ID查询用户表，一次性查出所有UP主信息
        List<User> userList = userService.listByIds(
                followerList.stream().map(Follow::getUserId).collect(Collectors.toSet())
        );
        // 将用户列表转为Map，方便快速查找
        // key = 用户ID(UP主ID)，value = User对象
        // 作用：根据ID快速查用户，不用循环遍历，速度极快（O(1)查找）
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
        List<UserListResponse> userListResponses = new ArrayList<>();
        // 遍历所有粉丝记录
        for (Follow follower : followerList) {
            UserListResponse userListResponse = new UserListResponse();
            userListResponse.setUserId(follower.getUserId());
            userListResponse.setAvatar(userMap.get(follower.getUserId()).getAvatar());
            userListResponse.setNickname(userMap.get(follower.getUserId()).getNickname());
            userListResponse.setDescription(userMap.get(follower.getUserId()).getDescription());
            // 添加到结果列表
            userListResponses.add(userListResponse);
        }
        return userListResponses;
    }
    //设置与up主关系：0 未关注 1 已关注 2 互相关注
    @Override
    public Integer getFollowType(Long userId, Long creatorId) {
        Integer followType = 0;
        boolean existsFollowing = this.lambdaQuery().eq(Follow::getUserId, userId).eq(Follow::getCreatorId, creatorId).exists();
        boolean existsFollower = this.lambdaQuery().eq(Follow::getUserId, creatorId).eq(Follow::getCreatorId, userId).exists();
        if (existsFollowing && existsFollower) {
            followType = 2;
        } else if (existsFollowing) {
            followType = 1;
        }
        return followType;
    }

}




