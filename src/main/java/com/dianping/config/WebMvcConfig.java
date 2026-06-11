package com.dianping.config;

import com.dianping.interceptor.LoginInterceptor;
import com.dianping.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 第一层：Token 刷新拦截器（所有路径）
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);

        // 第二层：登录校验拦截器（需要登录的路径）
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/api/user/code",
                        "/api/user/login",
                        "/api/admin/login",
                        "/api/shop-type/**",
                        "/api/shop/**",
                        "/api/voucher/list/**",
                        "/api/blog/likes/**",
                        "/api/uv/**",
                        "/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                )
                .order(1);
    }
}
