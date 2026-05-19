package com.shanyangcode.userservice.model.dto;


import lombok.Data;


/**
 * 关注用户 DTO
 */
@Data
public class UserInfoRequest {


    /**
     * 用户 ID
     */

    private Long userId;


    /**
     * 被关注者 ID
     */

    private Long creatorId;
}
