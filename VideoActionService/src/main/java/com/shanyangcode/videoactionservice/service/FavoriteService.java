package com.shanyangcode.videoactionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.CancelVideoActionRequest;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.entity.Favorite;


/**
* @author 717
* @description 针对表【favorite(收藏表)】的数据库操作Service
* @createDate 2026-05-07 16:30:37
*/
public interface FavoriteService extends IService<Favorite> {
    Long favoriteVideo(VideoActionRequest videoActionRequest);
    Boolean cancelFavoriteVideo(CancelVideoActionRequest cancelVideoActionRequest);
}
