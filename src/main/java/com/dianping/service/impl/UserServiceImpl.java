package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IUserService;
import com.dianping.utils.RegexUtils;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE);
        }
        String codeKey = "login:code:" + phone;
        String existed = stringRedisTemplate.opsForValue().get(codeKey);
        if (existed != null) {
            throw new BusinessException(ErrorCode.CODE_TOO_FREQUENT);
        }
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(codeKey, code, 5, TimeUnit.MINUTES);
        log.info("验证码 [{}]: {}", phone, code);
        return Result.ok(null);
    }

    @Override
    public Result login(LoginFormDTO form) {
        String phone = form.getPhone();
        String code = form.getCode();
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE);
        }
        if (StrUtil.isBlank(code) || !code.matches("\\d{6}")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String codeKey = "login:code:" + phone;
        String savedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (StrUtil.isBlank(savedCode)) {
            throw new BusinessException(ErrorCode.CODE_EXPIRED);
        }
        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_CODE);
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName("user_" + RandomUtil.randomString(6));
            userMapper.insert(user);
        }
        String token = UUID.randomUUID().toString();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        // Convert values to strings for Redis storage
        Map<String, String> stringMap = new HashMap<>();
        userMap.forEach((k, v) -> stringMap.put(k, v != null ? v.toString() : ""));
        String tokenKey = "login:token:" + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, stringMap);
        stringRedisTemplate.expire(tokenKey, 30, TimeUnit.MINUTES);
        stringRedisTemplate.delete(codeKey);
        return Result.ok(token);
    }

    @Override
    public Result me() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return Result.ok(user);
    }

    @Override
    public Result logout() {
        UserDTO user = UserHolder.getUser();
        if (user != null) {
            // Token is cleaned by interceptor
            UserHolder.remove();
        }
        return Result.ok(null);
    }
}
