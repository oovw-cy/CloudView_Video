package com.shanyangcode.userservice.model.vo;

import lombok.Data;

import java.io.Serializable;

//用户列表，用于拉去粉丝/关注列表
@Data
public class UserListResponse implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像url
     */
    private String avatar;


    /**
     * 个性签名
     */
    private String description;

}
