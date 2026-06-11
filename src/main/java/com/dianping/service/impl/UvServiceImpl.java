package com.dianping.service.impl;

import com.dianping.common.Result;
import com.dianping.service.IUvService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class UvServiceImpl implements IUvService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addUv(Long userId) {
        String key = "uv:day:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        stringRedisTemplate.opsForHyperLogLog().add(key, userId.toString());
    }

    @Override
    public Result getUv(String date) {
        String key = "uv:day:" + date.replace("-", "");
        Long uv = stringRedisTemplate.opsForHyperLogLog().size(key);
        return Result.ok(uv);
    }
}
