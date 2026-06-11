package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.Result;
import com.dianping.entity.Follow;
import com.dianping.entity.User;
import com.dianping.mapper.FollowMapper;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IFollowService;
import com.dianping.utils.UserHolder;
import com.dianping.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FollowServiceImpl implements IFollowService {

    @Resource
    private FollowMapper followMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result follow(Long followUserId, boolean isFollow) {
        Long userId = UserHolder.getUserId();
        if (userId.equals(followUserId)) {
            return Result.error("不能关注自己");
        }

        String followKey = "follow:" + userId;
        String fanKey = "fan:" + followUserId;

        if (isFollow) {
            Follow f = new Follow();
            f.setUserId(userId);
            f.setFollowUserId(followUserId);
            followMapper.insert(f);
            stringRedisTemplate.opsForSet().add(followKey, followUserId.toString());
            stringRedisTemplate.opsForSet().add(fanKey, userId.toString());
        } else {
            followMapper.delete(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, userId)
                    .eq(Follow::getFollowUserId, followUserId));
            stringRedisTemplate.opsForSet().remove(followKey, followUserId.toString());
            stringRedisTemplate.opsForSet().remove(fanKey, userId.toString());
        }
        return Result.ok(null);
    }

    @Override
    public boolean isFollow(Long followUserId) {
        Long userId = UserHolder.getUserId();
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
        return count != null && count > 0;
    }

    @Override
    public List<UserVO> commonFollow(Long userId) {
        Long curUserId = UserHolder.getUserId();
        String key1 = "follow:" + curUserId;
        String key2 = "follow:" + userId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) {
            return Collections.emptyList();
        }
        return intersect.stream().map(id -> {
            User user = userMapper.selectById(Long.valueOf(id));
            return user != null ? BeanUtil.copyProperties(user, UserVO.class) : null;
        }).filter(u -> u != null).collect(Collectors.toList());
    }

    @Override
    public List<UserVO> queryFollows(Long userId) {
        String key = "follow:" + userId;
        Set<String> ids = stringRedisTemplate.opsForSet().members(key);
        if (ids == null || ids.isEmpty()) {
            // Fallback to DB
            List<Follow> follows = followMapper.selectList(
                    new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId));
            return follows.stream().map(f -> {
                User user = userMapper.selectById(f.getFollowUserId());
                return user != null ? BeanUtil.copyProperties(user, UserVO.class) : null;
            }).filter(u -> u != null).collect(Collectors.toList());
        }
        return ids.stream().map(id -> {
            User user = userMapper.selectById(Long.valueOf(id));
            return user != null ? BeanUtil.copyProperties(user, UserVO.class) : null;
        }).filter(u -> u != null).collect(Collectors.toList());
    }
}
