package com.shanyangcode.userservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.JWTConstant;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.utils.JwtUtil;
import com.shanyangcode.userservice.client.VideoActionServiceClient;
import com.shanyangcode.userservice.constants.SMSConstant;
import com.shanyangcode.common.constant.UserConstant;
import com.shanyangcode.userservice.mapper.UserMapper;
import com.shanyangcode.userservice.model.dto.LoginCodeRequest;
import com.shanyangcode.userservice.model.dto.LoginPasswordRequest;
import com.shanyangcode.userservice.model.dto.RegisterRequest;
import com.shanyangcode.userservice.model.dto.UserInfoRequest;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserStats;
import com.shanyangcode.userservice.model.vo.LoginResponse;
import com.shanyangcode.userservice.model.vo.UserInfoResponse;
import com.shanyangcode.userservice.service.UserService;
import com.shanyangcode.userservice.service.UserStatsService;
import com.shanyangcode.userservice.utils.RandomCodeUtil;
import com.shanyangcode.userservice.utils.SendMailUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserStatsService userStatsService;

    @Resource
    private VideoActionServiceClient videoActionServiceClient;

//    @Resource
//    @Lazy
//    private FollowService followService;

    @Override
    public void sendVerificationCode(String account) {
        // 检查输入是否为空
        if (StringUtils.isBlank(account)) {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }
        // 生成验证码并发送
        String code = RandomCodeUtil.generateSixDigitRandomNumber();
        // 判断账号格式

        if (account.matches(UserConstant.EMAIL_REGEX)) {
            // 是邮箱 → 发送邮件验证码
            SendMailUtil.sendEmailCode(account, code);
        } else if (account.matches(UserConstant.PHONE_REGEX)) {
            //是手机号 → 抛出异常：本项目不支持手机号注册
            throw new BusinessException(ErrorCode.PHONE_REGISTRATION_NOT_SUPPORTED);
        } else {
            //既不是邮箱也不是手机号 → 抛出格式错误
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }
        // 保存验证码到 redis, 并设置过期时间
        stringRedisTemplate.opsForValue().set(account, code, SMSConstant.SMS_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest registerRequest, HttpServletRequest httpServletRequest) {
        //1. 参数校验
        validateRegisterRequest(registerRequest);

        //2. 验证码校验
        validateVerificationCode(registerRequest.getAccount(), registerRequest.getCode());

        //3. 检查用户是否已存在
        checkUserExistence(registerRequest.getAccount());

        //4. 创建用户
        User newUser = createUser(registerRequest);

        //5. 保存用户信息(并发安全处理)
        return saveUserAndGenerateToken(newUser, registerRequest.getAccount());
    }

    @Override
    public LoginResponse loginPassword(LoginPasswordRequest loginPasswordRequest, HttpServletRequest httpServletRequest) {
        String account = loginPasswordRequest.getAccount();
        String password = loginPasswordRequest.getPassword();

        // check account format
        validateAccountFormat(account);

        // try to get the current user
        User user = getCurrentUser(account);

        // check password
        String encryptedPassword = DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + password).getBytes());
        ThrowUtils.throwIf(!encryptedPassword.equals(user.getPassword()), ErrorCode.LOGIN_ERROR);

        // set user
        LoginResponse loginResponse = new LoginResponse();
        BeanUtil.copyProperties(user, loginResponse);

        // set user-stats
        UserStats userStats = userStatsService.getById(user.getUserId());
        BeanUtil.copyProperties(userStats, loginResponse);

        // generate and store jwt token
        String token = JwtUtil.generate(user.getUserId().toString()); //每一次登录都生成新的token
        stringRedisTemplate.opsForValue().set(user.getUserId().toString(), token, JWTConstant.JWT_TIME_OUT, TimeUnit.DAYS);

        loginResponse.setToken(token);
        return loginResponse;
    }
    @Override
    public LoginResponse loginCode(LoginCodeRequest loginCodeRequest, HttpServletRequest httpServletRequest) {
        String account = loginCodeRequest.getAccount();
        String code = loginCodeRequest.getCode();

        // check account format
        validateAccountFormat(account);

        // try to get the current user
        User user = getCurrentUser(account);

        // validate verification code
        String redisCode = stringRedisTemplate.opsForValue().get(account);
        ThrowUtils.throwIf(redisCode == null || !redisCode.equals(code), ErrorCode.LOGIN_ERROR_CODE);

        // set user
        LoginResponse loginResponse = new LoginResponse();
        BeanUtil.copyProperties(user, loginResponse);

        // set user-stats
        UserStats userStats = userStatsService.getById(user.getUserId());
        BeanUtil.copyProperties(userStats, loginResponse);

        // delete verification code stored in redis
        stringRedisTemplate.delete(account);

        // generate and store jwt token
        String token = JwtUtil.generate(user.getUserId().toString());
        stringRedisTemplate.opsForValue().set(user.getUserId().toString(), token, JWTConstant.JWT_TIME_OUT, TimeUnit.DAYS);

        loginResponse.setToken(token);
        return loginResponse;
    }

    //参数校验
    private void validateRegisterRequest(RegisterRequest registerRequest) {
        String account = registerRequest.getAccount();
        validateAccountFormat(account);
        if(StringUtils.isBlank(registerRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (StringUtils.isBlank(registerRequest.getNickname())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能为空");
        }
    }
    /**
     * 验证码校验
     */
    private void validateVerificationCode(String account, String code) {
        String redisCode = stringRedisTemplate.opsForValue().get(account);
        if (StringUtils.isBlank(redisCode) || !redisCode.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
    }

    /**
     * 检查用户是否已存在
     */
    private void checkUserExistence(String account) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (account.matches(UserConstant.EMAIL_REGEX)) {
            queryWrapper.eq(User::getEmail, account);
        }
        if (this.getOne(queryWrapper) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    /**
     * 创建用户实体
     */
    private User createUser(RegisterRequest request) {
        User user = new User();
        user.setUserId(IdUtil.getSnowflake().nextId());
        user.setNickname(request.getNickname());

        // 设置账号(手机号或邮箱)
        if (request.getAccount().matches(UserConstant.EMAIL_REGEX)) {
            user.setEmail(request.getAccount());
            //user.setPhone("");
            user.setPhone("手机号暂未支持使用"+IdUtil.getSnowflake().nextId());//避免多次创建用户时手机号均为""，手机号为唯一索引，此时报错
        }

        // 密码加密
        String password = request.getPassword();
        String encryptedPassword = DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + password).getBytes());
        user.setPassword(encryptedPassword);

        return user;
    }

    /**
     * 保存用户并生成Token(并发安全处理)
     */

    private LoginResponse saveUserAndGenerateToken(User user, String account) {
        synchronized (account.intern()) {
            // 保存用户
            boolean saveSuccess = this.baseMapper.insert(user) > 0;

            // 初始化用户统计信息
            UserStats stats = new UserStats();
            stats.setUserId(user.getUserId());
            boolean saveStatsSuccess = userStatsService.save(stats);

            if (!saveSuccess || !saveStatsSuccess) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
            }

            // 清理验证码
            stringRedisTemplate.delete(account);

            // 生成 Token
            String token = JwtUtil.generate(user.getUserId().toString());
            stringRedisTemplate.opsForValue().set(user.getUserId().toString(), token, JWTConstant.JWT_TIME_OUT, TimeUnit.DAYS);

            // 返回用户信息和 Token
            LoginResponse response = this.baseMapper.getUserInfo(user.getUserId());
            response.setToken(token);
            return response;
        }
    }

    /**
     * 校验请求参数合法性
     */
    private void validateAccountFormat(String account) {
        if (!account.matches(UserConstant.PHONE_REGEX) && !account.matches(UserConstant.EMAIL_REGEX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号必须是有效的手机号或邮箱");
        }
    }
    private User getCurrentUser(String account) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (account.matches(UserConstant.EMAIL_REGEX)) {
            queryWrapper.eq(User::getEmail, account);
        } else if (account.matches(UserConstant.PHONE_REGEX)) {
            queryWrapper.eq(User::getPhone, account);
        }

        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        return user;
    }
    //获取用户信息
    @Override
    public UserInfoResponse getUserInfo(UserInfoRequest userInfoRequest) {
        // 查询用户信息判断用户是否存在
        User user = this.getById(userInfoRequest.getCreatorId());
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXISTS);

        // 用户基本信息
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        BeanUtil.copyProperties(user, userInfoResponse);

        // 用户统计信息
        UserStats userStats = userStatsService.getById(userInfoRequest.getCreatorId());
        BeanUtil.copyProperties(userStats, userInfoResponse);

        Long userId = userInfoRequest.getUserId();
        if (userId != null) {
            // 用户关注状态
            userInfoResponse.setFollow(videoActionServiceClient.followType(userInfoRequest.getUserId(), userInfoRequest.getCreatorId()));
        } else {
            userInfoResponse.setFollow(0);
        }

        return userInfoResponse;
    }
    //用户登出
    @Override
    public boolean userLogout(Long userId, HttpServletRequest request) {
        stringRedisTemplate.delete(userId.toString());
        return true;
    }
}
