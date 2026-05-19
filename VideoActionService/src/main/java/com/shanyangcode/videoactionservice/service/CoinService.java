package com.shanyangcode.videoactionservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.videoactionservice.model.dto.VideoActionRequest;
import com.shanyangcode.videoactionservice.model.entity.Coin;


/**
* @author 717
* @description 针对表【coin(投币表)】的数据库操作Service
* @createDate 2026-05-07 16:30:37
*/
public interface CoinService extends IService<Coin> {
    Boolean coinVideo(VideoActionRequest videoActionRequest);
}
