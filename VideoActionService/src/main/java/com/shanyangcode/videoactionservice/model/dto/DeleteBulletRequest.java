package com.shanyangcode.videoactionservice.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除弹幕 DTO
 */
@Data
public class DeleteBulletRequest implements Serializable {

    /**
     * 弹幕 id
     */
    private Long bulletId;


    /**
     *  uid
     */
    private Long userId;


    /**
     *  video id
     */
    private Long videoId;


    /**
     *  内容
     */
    private String content;



}
