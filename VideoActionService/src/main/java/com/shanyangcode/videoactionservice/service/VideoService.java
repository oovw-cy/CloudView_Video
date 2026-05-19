package com.shanyangcode.videoactionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.VideoSubmitRequest;
import com.shanyangcode.videoactionservice.model.entity.Video;
import com.shanyangcode.videoactionservice.model.vo.FavoriteVideoResponse;
import com.shanyangcode.videoactionservice.model.vo.TripleActionResponse;
import com.shanyangcode.videoactionservice.model.vo.VideoListResponse;
import com.shanyangcode.videoactionservice.model.vo.VideoResponse;


import java.util.List;


/**
* @author 717
* @description 针对表【video(视频表)】的数据库操作Service
* @createDate 2026-05-07 09:28:33
*/
public interface VideoService extends IService<Video> {
    boolean submit(VideoSubmitRequest videoSubmitRequest) throws Exception;
    List<VideoListResponse> getVideoList(Integer current, Integer pageSize);
    VideoResponse videoDetail(VideoActionRequest videoActionRequest);
    List<VideoListResponse> getSubmitVideoList(Long userId);
    List<VideoListResponse> getCategoryVideoList(Integer categoryId);
    TripleActionResponse tripleAction(VideoActionRequest videoActionRequest);
    List<VideoListResponse> getCoinVideoList(Long userId);
    List<VideoListResponse> getLikeVideoList(Long userId);
    List<FavoriteVideoResponse> getFavoriteVideoList(Long userId);
}
