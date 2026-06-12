package com.dianping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Blog;
import com.dianping.service.IBlogService;
import com.dianping.vo.BlogVO;
import com.dianping.vo.UserVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result save(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @GetMapping
    public Result<Page<BlogVO>> list(@RequestParam(defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryBlogByPage(current));
    }

    @GetMapping("/{id}")
    public Result<BlogVO> detail(@PathVariable Long id) {
        return Result.ok(blogService.queryBlogById(id));
    }

    @PutMapping("/like/{id}")
    public Result like(@PathVariable Long id) {
        return blogService.likeBlog(id);
    }

    @GetMapping("/likes/{id}")
    public Result<List<UserVO>> likes(@PathVariable Long id) {
        return Result.ok(blogService.queryBlogLikes(id));
    }

    @GetMapping("/follow")
    public Result follow(
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(lastId, offset);
    }
}
