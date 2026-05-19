package com.shanyangcode.videoactionservice.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.FollowRequest;
import com.shanyangcode.videoactionservice.model.entity.Follow;
import com.shanyangcode.videoactionservice.model.vo.UserListResponse;

import java.util.List;


/**
* @author 717
* @description 针对表【follow(关注表)】的数据库操作Service
* @createDate 2026-05-07 22:36:28
*/
public interface FollowService extends IService<Follow> {
    boolean follow(FollowRequest focusRequest);
    boolean chanelFollow(FollowRequest focusRequest);
    List<UserListResponse> followList(Long userId);
    List<UserListResponse> followerList(Long userId);
    Integer getFollowType(Long userId, Long creatorId);

}
