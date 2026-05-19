package com.shanyangcode.common.model.vo;

import lombok.Data;

import java.io.Serializable;


@Data
public class OnlineBulletResponse implements Serializable {

    /**
     * 弹幕内容
     */
    private String text;


    /**
     * 弹幕所在视频的时间点
     */
    private Double playbackTime;


    /**
     * 用户 id
     */
    private String userId;

    /**
     * 弹幕 id
     */
    private String bulletId;
}
