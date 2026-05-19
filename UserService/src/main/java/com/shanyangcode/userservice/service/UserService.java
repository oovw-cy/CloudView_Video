package com.shanyangcode.userservice.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.userservice.model.dto.LoginCodeRequest;
import com.shanyangcode.userservice.model.dto.LoginPasswordRequest;
import com.shanyangcode.userservice.model.dto.RegisterRequest;
import com.shanyangcode.userservice.model.dto.UserInfoRequest;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.vo.LoginResponse;
import com.shanyangcode.userservice.model.vo.UserInfoResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
    void sendVerificationCode(String account);
    LoginResponse register(RegisterRequest registerRequest, HttpServletRequest httpServletRequest);
    LoginResponse loginPassword(LoginPasswordRequest loginPasswordRequest, HttpServletRequest httpServletRequest);

    LoginResponse loginCode(LoginCodeRequest loginCodeRequest, HttpServletRequest httpServletRequest);

    boolean userLogout(Long userId, HttpServletRequest request);

    UserInfoResponse getUserInfo(UserInfoRequest userInfoRequest);
}
