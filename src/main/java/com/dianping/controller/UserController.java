package com.dianping.controller;

import com.dianping.annotation.RateLimiter;
import com.dianping.common.Result;
import com.dianping.dto.LoginFormDTO;
import com.dianping.service.IUserService;
import com.dianping.service.IUvService;
import com.dianping.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUvService uvService;

    @PostMapping("/code")
    public Result sendCode(@RequestBody LoginFormDTO form) {
        return userService.sendCode(form.getPhone());
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginFormDTO form) {
        Result result = userService.login(form);
        // Track UV after login
        if (result.getSuccess() && UserHolder.getUserId() != null) {
            uvService.addUv(UserHolder.getUserId());
        }
        return result;
    }

    @GetMapping("/me")
    public Result me() {
        return userService.me();
    }

    @PostMapping("/logout")
    public Result logout() {
        return userService.logout();
    }
}
