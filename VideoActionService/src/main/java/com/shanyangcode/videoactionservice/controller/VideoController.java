package com.shanyangcode.videoactionservice.controller;


import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.videoactionservice.model.dto.CancelVideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.CreateCommentRequest;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.VideoSubmitRequest;
import com.shanyangcode.videoactionservice.model.vo.*;
import com.shanyangcode.videoactionservice.service.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/video")
@Slf4j
public class VideoController {

    @Resource
    private VideoService videoService;
    @Resource
    private LikeService likeService;
    @Resource
    private CoinService coinService;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private CommentService commentService;
    //投稿视频
    @PostMapping("/submit")
    public BaseResponse<Boolean> submit(@RequestParam String fileUrl, @RequestParam Long userId, @RequestParam MultipartFile file, @RequestParam String title, @RequestParam Integer type, @RequestParam Double duration, @RequestParam Integer categoryId, @RequestParam String tags, @RequestParam String description) throws Exception {
        VideoSubmitRequest videoSubmitRequest = new VideoSubmitRequest(fileUrl, userId, file, title, type, duration, categoryId, tags, description);
        return ResultUtils.success(videoService.submit(videoSubmitRequest));
    }
    //获取首页/各个页的视频列表
    @GetMapping("/list")
    public BaseResponse<List<VideoListResponse>> videoList(@RequestParam Integer current, @RequestParam Integer pageSize) {
        return ResultUtils.success(videoService.getVideoList(current, pageSize));
    }
    //获取视频详细信息
    @PostMapping("/detail")
    public BaseResponse<VideoResponse> videoDetail(@RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(videoService.videoDetail(videoActionRequest));
    }
    //获取投稿视频列表
    @GetMapping("/submit/list")
    public BaseResponse<List<VideoListResponse>> submitVideoList(@Valid @NotNull(message = "用户ID不能为空") @RequestParam Long userId) {
        return ResultUtils.success(videoService.getSubmitVideoList(userId));
    }
    //视频点赞
    @PostMapping("/like")
    public BaseResponse<Long> likeVideo(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(likeService.likeVideo(videoActionRequest));
    }
    //取消点赞
    @PostMapping("/cancel/like")
    public BaseResponse<Boolean> cancelLikeVideo(@Valid @RequestBody CancelVideoActionRequest cancelVideoActionRequest) {
        return ResultUtils.success(likeService.cancelLikeVideo(cancelVideoActionRequest));
    }
    //视频投币
    @PostMapping("/coin")
    public BaseResponse<Boolean> coinVideo(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(coinService.coinVideo(videoActionRequest));
    }
    //视频收藏
    @PostMapping("/favorite")
    public BaseResponse<Long> favoriteVideo(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(favoriteService.favoriteVideo(videoActionRequest));
    }
    //取消收藏
    @PostMapping("/cancel/favorite")
    public BaseResponse<Boolean> cancelFavoriteVideo(@Valid @RequestBody CancelVideoActionRequest cancelVideoActionRequest) {
        return ResultUtils.success(favoriteService.cancelFavoriteVideo(cancelVideoActionRequest));
    }
    //创建评论
    @PostMapping("/create/comment")
    public BaseResponse<CommentResponse> createCommentVideo(@Valid @RequestBody CreateCommentRequest createCommentRequest) {
        return ResultUtils.success(commentService.createCommentVideo(createCommentRequest));
    }
    //删除评论
    @PostMapping("/delete/comment")
    public BaseResponse<Boolean> deleteCommentVideo(@Valid @RequestBody CancelVideoActionRequest cancelVideoActionRequest) {
        return ResultUtils.success(commentService.deleteCommentVideo(cancelVideoActionRequest));
    }
    //查询视频下的评论列表
    @GetMapping("/comment/list")
    public BaseResponse<List<CommentVideoResponse>> getCommentVideoList(@NotNull(message = "视频id不能为空") @RequestParam Long videoId) {
        return ResultUtils.success(commentService.getCommentVideoList(videoId));
    }
    //视频一键三连
    @PostMapping("/triple/action")
    public BaseResponse<TripleActionResponse> tripleAction(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(videoService.tripleAction(videoActionRequest));
    }
    //获取收藏列表
    @GetMapping("/favorite/list")
    public BaseResponse<List<FavoriteVideoResponse>> favoriteVideoList(@NotNull(message = "用户ID不能为空") @RequestParam Long userId) {
        return ResultUtils.success(videoService.getFavoriteVideoList(userId));
    }
    //获取喜欢列表
    @GetMapping("/like/list")
    public BaseResponse<List<VideoListResponse>> likeVideoList(@NotNull(message = "用户ID不能为空") @RequestParam Long userId) {
        return ResultUtils.success(videoService.getLikeVideoList(userId));
    }
    //获取投币列表
    @GetMapping("/coin/list")
    public BaseResponse<List<VideoListResponse>> coinVideoList(@NotNull(message = "用户ID不能为空") @RequestParam Long userId) {
        return ResultUtils.success(videoService.getCoinVideoList(userId));
    }


}
