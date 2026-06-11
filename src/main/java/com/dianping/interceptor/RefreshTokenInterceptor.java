package com.dianping.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dianping.dto.UserDTO;
import com.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private static final long TOKEN_TTL = 30L;
    private final StringRedisTemplate redisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // Handle admin token
        if (token.startsWith("admin:")) {
            String key = "admin:token:" + token;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                redisTemplate.expire(key, 60, TimeUnit.MINUTES);
                // Set a simple auth marker for admin
                UserDTO adminDTO = new UserDTO();
                if (entries.get("id") != null) {
                    adminDTO.setId(Long.valueOf((String) entries.get("id")));
                }
                adminDTO.setNickName((String) entries.get("nickname"));
                UserHolder.saveUser(adminDTO);
            }
            return true;
        }
        // Handle user token
        String key = "login:token:" + token;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return true;
        }
        redisTemplate.expire(key, TOKEN_TTL, TimeUnit.MINUTES);
        UserDTO userDTO = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.remove();
    }
}
