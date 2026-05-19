package com.shanyangcode.videoactionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.shanyangcode.videoactionservice.model.entity.Video;
import com.shanyangcode.videoactionservice.model.vo.FavoriteVideoResponse;
import com.shanyangcode.videoactionservice.model.vo.VideoDetailsResponse;
import com.shanyangcode.videoactionservice.model.vo.VideoListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
* @author 717
* @description 针对表【video(视频表)】的数据库操作Mapper
* @createDate 2026-05-07 09:28:33
* @Entity generator.domain.Video
*/
@Mapper
public interface VideoMapper extends BaseMapper<Video> {
    List<VideoListResponse> selectVideoWithStats(@Param("current") Integer current, @Param("pageSize") Integer pageSize);
    List<VideoListResponse> recommendVideoList(@Param("categoryId") Integer categoryId, @Param("videoId") Long vid);

    VideoDetailsResponse getVideoDetails(Long videoId);

    List<VideoListResponse> getSubmitVideoList(Long videoId);

    List<VideoListResponse> getCategoryVideoList(Integer categoryId);

    List<VideoListResponse> getLikeVideoList(Long userId);

    List<VideoListResponse> getCoinVideoList(Long userId);

    List<FavoriteVideoResponse> getFavoriteVideoList(Long userId);
}




