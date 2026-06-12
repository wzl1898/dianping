package com.dianping.controller;

import com.dianping.common.Result;
import com.dianping.service.IFollowService;
import com.dianping.vo.UserVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PutMapping("/{userId}/{isFollow}")
    public Result follow(@PathVariable Long userId, @PathVariable Boolean isFollow) {
        return followService.follow(userId, isFollow);
    }

    @GetMapping("/or/not/{userId}")
    public Result<Boolean> isFollow(@PathVariable Long userId) {
        return Result.ok(followService.isFollow(userId));
    }

    @GetMapping("/common/{userId}")
    public Result<List<UserVO>> common(@PathVariable Long userId) {
        return Result.ok(followService.commonFollow(userId));
    }
}
