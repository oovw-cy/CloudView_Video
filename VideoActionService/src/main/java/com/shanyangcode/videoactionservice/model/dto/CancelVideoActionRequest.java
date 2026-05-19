package com.shanyangcode.videoactionservice.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CancelVideoActionRequest implements Serializable {


    /**
     * 视频点赞，收藏，评论的 id
     */
    private Long id;


    /**
     * 视频 id
     */
    private Long videoId;
}
