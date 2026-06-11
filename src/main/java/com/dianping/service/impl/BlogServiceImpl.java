package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.entity.Blog;
import com.dianping.entity.Follow;
import com.dianping.entity.User;
import com.dianping.mapper.BlogMapper;
import com.dianping.mapper.FollowMapper;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IBlogService;
import com.dianping.utils.UserHolder;
import com.dianping.vo.BlogVO;
import com.dianping.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BlogServiceImpl implements IBlogService {

    @Resource
    private BlogMapper blogMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result saveBlog(Blog blog) {
        Long userId = UserHolder.getUserId();
        blog.setUserId(userId);
        blog.setLiked(0);
        blog.setComments(0);
        blogMapper.insert(blog);

        // Push to followers' feed
        List<Follow> followers = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowUserId, userId));
        for (Follow f : followers) {
            String feedKey = "feed:" + f.getUserId();
            stringRedisTemplate.opsForZSet().add(
                    feedKey, blog.getId().toString(), System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    @Override
    public BlogVO queryBlogById(Long id) {
        Blog blog = blogMapper.selectById(id);
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        return toBlogVO(blog);
    }

    @Override
    public Page<BlogVO> queryBlogByPage(Integer current) {
        Page<Blog> page = blogMapper.selectPage(
                new Page<>(current, 10),
                new LambdaQueryWrapper<Blog>().orderByDesc(Blog::getCreateTime));
        Page<BlogVO> voPage = new Page<>();
        BeanUtil.copyProperties(page, voPage, "records");
        List<BlogVO> voList = page.getRecords().stream()
                .map(this::toBlogVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUserId();
        String likeKey = "blog:liked:" + id;
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(likeKey, userId.toString());
        if (Boolean.TRUE.equals(isLiked)) {
            stringRedisTemplate.opsForSet().remove(likeKey, userId.toString());
            blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                    .eq(Blog::getId, id).setSql("liked = liked - 1"));
        } else {
            stringRedisTemplate.opsForSet().add(likeKey, userId.toString());
            blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                    .eq(Blog::getId, id).setSql("liked = liked + 1"));
            // Add to ranking
            stringRedisTemplate.opsForZSet().add(
                    "blog:liked:rank:" + id, userId.toString(), System.currentTimeMillis());
        }
        return Result.ok(null);
    }

    @Override
    public List<UserVO> queryBlogLikes(Long id) {
        Set<String> top5 = stringRedisTemplate.opsForSet().members("blog:liked:" + id);
        if (top5 == null || top5.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserVO> list = new ArrayList<>();
        int count = 0;
        for (String uid : top5) {
            if (count >= 5) break;
            User user = userMapper.selectById(Long.valueOf(uid));
            if (user != null) {
                list.add(BeanUtil.copyProperties(user, UserVO.class));
            }
            count++;
        }
        return list;
    }

    @Override
    public Result queryBlogOfFollow(Long lastId, Integer offset) {
        Long userId = UserHolder.getUserId();
        String feedKey = "feed:" + userId;

        Set<String> blogIds = stringRedisTemplate.opsForZSet()
                .reverseRangeByScore(feedKey, 0, lastId == null ? System.currentTimeMillis() : lastId,
                        offset == null ? 0 : offset, 5);

        if (blogIds == null || blogIds.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        List<BlogVO> voList = new ArrayList<>();
        for (String bid : blogIds) {
            Blog blog = blogMapper.selectById(Long.valueOf(bid));
            if (blog != null) {
                voList.add(toBlogVO(blog));
            }
        }
        return Result.ok(voList);
    }

    private BlogVO toBlogVO(Blog blog) {
        BlogVO vo = BeanUtil.copyProperties(blog, BlogVO.class);
        User user = userMapper.selectById(blog.getUserId());
        if (user != null) {
            UserVO author = BeanUtil.copyProperties(user, UserVO.class);
            vo.setAuthor(author);
        }
        Long curUserId = UserHolder.getUserId();
        if (curUserId != null) {
            Boolean liked = stringRedisTemplate.opsForSet()
                    .isMember("blog:liked:" + blog.getId(), curUserId.toString());
            vo.setIsLiked(Boolean.TRUE.equals(liked));
        }
        return vo;
    }
}
