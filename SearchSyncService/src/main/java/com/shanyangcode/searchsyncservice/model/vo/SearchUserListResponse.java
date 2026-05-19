package com.shanyangcode.searchsyncservice.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchUserListResponse implements Serializable {


    private Long userId;

    private String avatar;

    private String nickname;

    private String description;

    private Integer followers;

    private Integer videoCount;

}