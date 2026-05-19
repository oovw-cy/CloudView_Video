package com.shanyangcode.userservice.controller;


import com.shanyangcode.common.common.BaseResponse;
import com.shanyangcode.common.common.ResultUtils;
import com.shanyangcode.userservice.constants.SMSConstant;
import com.shanyangcode.userservice.model.dto.LoginCodeRequest;
import com.shanyangcode.userservice.model.dto.LoginPasswordRequest;
import com.shanyangcode.userservice.model.dto.RegisterRequest;
import com.shanyangcode.userservice.model.dto.UserInfoRequest;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.vo.LoginResponse;
import com.shanyangcode.userservice.model.vo.UserInfoResponse;
import com.shanyangcode.userservice.model.vo.UserListResponse;
import com.shanyangcode.userservice.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;



    //获取所有用户
    @GetMapping
    public List<User> listAllUsers() {
        return  userService.list();
    }

    //根据ID获取用户
    @GetMapping("/{userId}")
    public User getUserById(@PathVariable Long userId) {
        return userService.getById(userId);
    }

    //发送验证码
    @GetMapping("/sendVerificationCode")
    public BaseResponse<String> sendVerificationCode(@RequestParam String account) {
        userService.sendVerificationCode(account);
        return ResultUtils.success(SMSConstant.SMS_SEND_SUCCESS_MSG);
    }

    //注册用户
    @PostMapping("/register")
    public BaseResponse<LoginResponse> register(@RequestBody RegisterRequest registerRequest, HttpServletRequest httpServletRequest) {
        return ResultUtils.success(userService.register(registerRequest, httpServletRequest));
    }

    //密码登录
    @PostMapping("/loginPassword")
    public BaseResponse<LoginResponse> loginPassword(@Valid @RequestBody LoginPasswordRequest loginPasswordRequest, HttpServletRequest request) {
        return ResultUtils.success(userService.loginPassword(loginPasswordRequest,request));
    }

    //验证码登录
    @PostMapping("/loginCode")
    public BaseResponse<LoginResponse> loginCode(@Valid @RequestBody LoginCodeRequest loginCodeRequest, HttpServletRequest request) {
        return ResultUtils.success(userService.loginCode(loginCodeRequest,request));
    }
    //登出
    @GetMapping("/logout")
    public BaseResponse<Boolean> logout(@Valid @NotNull(message = "手用户id不能为空") @RequestParam Long userId, HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(userId, request));
    }
    //用户详细信息
    @PostMapping("/info")
    public BaseResponse<UserInfoResponse> getUserInfo(@RequestBody UserInfoRequest userInfoRequest) {
        return ResultUtils.success(userService.getUserInfo(userInfoRequest));
    }



//    //添加一条用户信息
//    @PostMapping("/add")
//    public void setUserById(@RequestBody User user) {
//        userService.save(user);
//    }

}
