package com.shanyangcode.userservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.JWTConstant;
import com.shanyangcode.common.constant.UserConstant;
import com.shanyangcode.common.exception.BusinessException;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.utils.JwtUtil;
import com.shanyangcode.userservice.client.VideoActionServiceClient;
import com.shanyangcode.userservice.constants.SMSConstant;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String VERIFY_CODE_KEY_PREFIX = "user:verify-code:";
    private static final String UNSUPPORTED_PHONE_PREFIX = "UNSUPPORTED_PHONE_";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserStatsService userStatsService;
    private final VideoActionServiceClient videoActionServiceClient;

    @Override
    public void sendVerificationCode(String account) {
        validateVerificationCodeAccount(account);

        String code = RandomCodeUtil.generateSixDigitRandomNumber();
        SendMailUtil.sendEmailCode(account, code);

        stringRedisTemplate.opsForValue()
                .set(buildVerifyCodeKey(account), code, SMSConstant.SMS_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest registerRequest, HttpServletRequest httpServletRequest) {
        validateRegisterRequest(registerRequest);

        String account = registerRequest.getAccount();
        validateVerificationCode(account, registerRequest.getCode());
        checkUserExistence(account);

        User user = createUser(registerRequest);
        return saveUserAndBuildLoginResponse(user, account);
    }

    @Override
    public LoginResponse loginPassword(LoginPasswordRequest loginPasswordRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(loginPasswordRequest == null, ErrorCode.PARAMS_ERROR);

        String account = loginPasswordRequest.getAccount();
        String password = loginPasswordRequest.getPassword();

        validateAccountFormat(account);
        ThrowUtils.throwIf(StringUtils.isBlank(password), ErrorCode.LOGIN_ERROR);

        User user = getCurrentUser(account);
        ThrowUtils.throwIf(!encryptPassword(password).equals(user.getPassword()), ErrorCode.LOGIN_ERROR);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse loginCode(LoginCodeRequest loginCodeRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(loginCodeRequest == null, ErrorCode.PARAMS_ERROR);

        String account = loginCodeRequest.getAccount();
        String code = loginCodeRequest.getCode();

        validateAccountFormat(account);

        User user = getCurrentUser(account);
        String redisCode = getVerificationCode(account);
        ThrowUtils.throwIf(StringUtils.isBlank(redisCode) || !redisCode.equals(code), ErrorCode.LOGIN_ERROR_CODE);

        stringRedisTemplate.delete(buildVerifyCodeKey(account));
        return buildLoginResponse(user);
    }

    @Override
    public UserInfoResponse getUserInfo(UserInfoRequest userInfoRequest) {
        ThrowUtils.throwIf(userInfoRequest == null || userInfoRequest.getCreatorId() == null, ErrorCode.PARAMS_ERROR);

        Long creatorId = userInfoRequest.getCreatorId();
        User user = this.getById(creatorId);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXISTS);

        UserInfoResponse response = new UserInfoResponse();
        BeanUtil.copyProperties(user, response);
        copyUserStats(creatorId, response);

        Long userId = userInfoRequest.getUserId();
        response.setFollow(userId == null ? 0 : videoActionServiceClient.followType(userId, creatorId));
        return response;
    }

    @Override
    public boolean userLogout(Long userId, HttpServletRequest request) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);

        stringRedisTemplate.delete(buildTokenKey(userId));
        return true;
    }

    private void validateRegisterRequest(RegisterRequest registerRequest) {
        ThrowUtils.throwIf(registerRequest == null, ErrorCode.PARAMS_ERROR);

        String account = registerRequest.getAccount();
        if (StringUtils.isBlank(account) || !isEmail(account)) {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }
        if (StringUtils.isBlank(registerRequest.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "password cannot be blank");
        }
        if (StringUtils.isBlank(registerRequest.getNickname())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "nickname cannot be blank");
        }
    }

    private void validateVerificationCodeAccount(String account) {
        if (StringUtils.isBlank(account)) {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }
        if (isPhone(account)) {
            throw new BusinessException(ErrorCode.PHONE_REGISTRATION_NOT_SUPPORTED);
        }
        if (!isEmail(account)) {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }
    }

    private void validateVerificationCode(String account, String code) {
        String redisCode = getVerificationCode(account);
        if (StringUtils.isBlank(code) || StringUtils.isBlank(redisCode) || !redisCode.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
    }

    private void validateAccountFormat(String account) {
        if (StringUtils.isBlank(account) || (!isPhone(account) && !isEmail(account))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "account must be a valid phone number or email");
        }
    }

    private void checkUserExistence(String account) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(User::getEmail, account);
        ThrowUtils.throwIf(this.count(queryWrapper) > 0, ErrorCode.USER_ALREADY_EXISTS);
    }

    private User createUser(RegisterRequest request) {
        Long userId = IdUtil.getSnowflake().nextId();

        User user = new User();
        user.setUserId(userId);
        user.setNickname(request.getNickname());
        user.setEmail(request.getAccount());
        user.setPhone(UNSUPPORTED_PHONE_PREFIX + userId);
        user.setPassword(encryptPassword(request.getPassword()));
        return user;
    }

    private LoginResponse saveUserAndBuildLoginResponse(User user, String account) {
        try {
            boolean saveUserSuccess = this.save(user);
            boolean saveStatsSuccess = initUserStats(user.getUserId());
            ThrowUtils.throwIf(!saveUserSuccess || !saveStatsSuccess, ErrorCode.SYSTEM_ERROR);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        stringRedisTemplate.delete(buildVerifyCodeKey(account));

        LoginResponse response = this.baseMapper.getUserInfo(user.getUserId());
        if (response == null) {
            response = buildLoginResponseWithoutToken(user);
        }
        response.setToken(issueToken(user.getUserId()));
        return response;
    }

    private boolean initUserStats(Long userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        return userStatsService.save(stats);
    }

    private User getCurrentUser(String account) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (isEmail(account)) {
            queryWrapper.eq(User::getEmail, account);
        } else {
            queryWrapper.eq(User::getPhone, account);
        }

        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXISTS);
        return user;
    }

    private LoginResponse buildLoginResponse(User user) {
        LoginResponse response = buildLoginResponseWithoutToken(user);
        response.setToken(issueToken(user.getUserId()));
        return response;
    }

    private LoginResponse buildLoginResponseWithoutToken(User user) {
        LoginResponse response = new LoginResponse();
        BeanUtil.copyProperties(user, response);
        copyUserStats(user.getUserId(), response);
        return response;
    }

    private void copyUserStats(Long userId, Object target) {
        UserStats userStats = userStatsService.getById(userId);
        if (userStats != null) {
            BeanUtil.copyProperties(userStats, target);
        }
    }

    private String issueToken(Long userId) {
        String token = JwtUtil.generate(userId.toString());
        stringRedisTemplate.opsForValue().set(buildTokenKey(userId), token, JWTConstant.JWT_TIME_OUT, TimeUnit.DAYS);
        return token;
    }

    private String getVerificationCode(String account) {
        return stringRedisTemplate.opsForValue().get(buildVerifyCodeKey(account));
    }

    private String buildVerifyCodeKey(String account) {
        return VERIFY_CODE_KEY_PREFIX + account;
    }

    private String buildTokenKey(Long userId) {
        return userId.toString();
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + password).getBytes(StandardCharsets.UTF_8));
    }

    private boolean isEmail(String account) {
        return !StringUtils.isBlank(account) && account.matches(UserConstant.EMAIL_REGEX);
    }

    private boolean isPhone(String account) {
        return !StringUtils.isBlank(account) && account.matches(UserConstant.PHONE_REGEX);
    }
}
