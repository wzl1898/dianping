package com.dianping.aop;

import cn.hutool.core.util.StrUtil;
import com.dianping.annotation.RateLimiter;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint pjp, RateLimiter rateLimiter) throws Throwable {
        String key = buildKey(rateLimiter);
        long now = System.currentTimeMillis();
        long windowStart = now - rateLimiter.time() * 1000L;

        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = stringRedisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= rateLimiter.count()) {
            log.warn("Rate limited: key={}, count={}", key, count);
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }

        stringRedisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        stringRedisTemplate.expire(key, rateLimiter.time(), TimeUnit.SECONDS);

        return pjp.proceed();
    }

    private String buildKey(RateLimiter rateLimiter) {
        String prefix = rateLimiter.key();
        String suffix;
        if (rateLimiter.limitType() == RateLimiter.LimitType.USER) {
            Long userId = UserHolder.getUserId();
            suffix = userId != null ? "user:" + userId : "unknown";
        } else {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                suffix = "ip:" + getIpAddr(request);
            } else {
                suffix = "unknown";
            }
        }
        return prefix + ":" + suffix;
    }

    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
