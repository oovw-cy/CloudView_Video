package com.shanyangcode.videoactionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.CancelVideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.CreateCommentRequest;
import com.shanyangcode.videoactionservice.model.entity.Comment;
import com.shanyangcode.videoactionservice.model.vo.CommentResponse;
import com.shanyangcode.videoactionservice.model.vo.CommentVideoResponse;


import java.util.List;


/**
* @author 717
* @description 针对表【comment(评论表)】的数据库操作Service
* @createDate 2026-05-07 22:36:28
*/
public interface CommentService extends IService<Comment> {
    CommentResponse createCommentVideo(CreateCommentRequest createCommentRequest);
    Boolean deleteCommentVideo(CancelVideoActionRequest cancelVideoActionRequest);
    List<CommentVideoResponse> getCommentVideoList(Long videoId);
}
