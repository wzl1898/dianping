package com.dianping.service.impl;

import com.dianping.common.Result;
import com.dianping.service.ISignService;
import com.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class SignServiceImpl implements ISignService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sign() {
        Long userId = UserHolder.getUserId();
        LocalDate now = LocalDate.now();
        String key = "sign:" + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int offset = now.getDayOfMonth() - 1;
        Boolean signed = stringRedisTemplate.opsForValue().setBit(key, offset, true);
        return Result.ok(signed != null && signed);
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.getUserId();
        LocalDate now = LocalDate.now();
        String key = "sign:" + userId + ":" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        // Total sign days this month
        Long total = stringRedisTemplate.execute(
                connection -> connection.bitCount(key.getBytes()),
                true);

        // Continuous sign days
        int continuous = 0;
        int today = now.getDayOfMonth();
        for (int i = today; i >= 1; i--) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(key, i - 1);
            if (Boolean.TRUE.equals(bit)) {
                continuous++;
            } else {
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total.intValue() : 0);
        result.put("continuous", continuous);
        return Result.ok(result);
    }
}
