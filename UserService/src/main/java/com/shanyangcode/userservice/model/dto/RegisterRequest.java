package com.shanyangcode.userservice.model.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String account;

    private String password;

    private String Code;

    private String nickname;
}
