package com.shanyangcode.videoactionservice.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

@Data
@SuppressWarnings({"all"})//关闭 IDEA（编译器）的所有代码警告，让黄色警告线 / 警告标记全部消失，代码看起来干干净净
public class SendBulletRequest implements Serializable {

    /**
     * 弹幕 ID
     */
    private Long bulletId;

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 弹幕内容
     */
    private String content;


    /**
     * 弹幕所在视频的时间点
     */
    private Double playbackTime;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
