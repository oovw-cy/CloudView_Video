package com.shanyangcode.videoactionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.CancelVideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.entity.Like;


/**
* @author 717
* @description 针对表【like(点赞表)】的数据库操作Service
* @createDate 2026-05-07 16:30:37
*/
public interface LikeService extends IService<Like> {

    Long likeVideo(VideoActionRequest videoActionRequest);

    Boolean cancelLikeVideo(CancelVideoActionRequest cancelVideoActionRequest);

}
