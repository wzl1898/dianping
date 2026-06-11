package com.dianping.controller.admin;

import com.dianping.common.Result;
import com.dianping.service.IAdminService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminLoginController {

    @Resource
    private IAdminService adminService;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        return adminService.login(params.get("username"), params.get("password"));
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("admin:")) {
            return adminService.logout(token);
        }
        return Result.error("未登录");
    }
}
