package com.dianping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Blog;
import com.dianping.vo.BlogVO;
import com.dianping.vo.UserVO;
import java.util.List;

public interface IBlogService {
    Result saveBlog(Blog blog);
    BlogVO queryBlogById(Long id);
    Page<BlogVO> queryBlogByPage(Integer current);
    Result likeBlog(Long id);
    List<UserVO> queryBlogLikes(Long id);
    Result queryBlogOfFollow(Long lastId, Integer offset);
}
