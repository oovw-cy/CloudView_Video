package com.shanyangcode.videoactionservice.controller;


import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.videoactionservice.model.dto.FollowRequest;
import com.shanyangcode.videoactionservice.model.vo.UserListResponse;
import com.shanyangcode.videoactionservice.service.FollowService;
import jakarta.annotation.Resource;

import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/video/user")
public class FollowController {

    @Resource
    private FollowService followService;



    //关注uo主
    @PostMapping("/follow")
    public BaseResponse<Boolean> follow(@RequestBody FollowRequest followRequest) {
        return ResultUtils.success(followService.follow(followRequest));
    }
    //取消关注
    @PostMapping("/chanel/follow")
    public BaseResponse<Boolean> chanelFollow(@RequestBody FollowRequest followRequest) {
        return ResultUtils.success(followService.chanelFollow(followRequest));
    }
    //查询关注列表
    @GetMapping("/following/list")
    public BaseResponse<List<UserListResponse>> followingList(@NotNull(message = "用户id不能为空") @RequestParam Long userId) {
        return ResultUtils.success(followService.followList(userId));
    }
    //查询粉丝列表
    @GetMapping("/followers/list")
    public BaseResponse<List<UserListResponse>> followersList(@NotNull(message = "用户id不能为空") @RequestParam Long userId) {
        return ResultUtils.success(followService.followerList(userId));
    }

    @PostMapping("/follow/type")
    public Integer followType(@RequestParam Long userId, @RequestParam Long creatorId) {
        return followService.getFollowType(userId, creatorId);
    }


}
