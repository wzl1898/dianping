package com.dianping.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.User;
import com.dianping.service.IAdminService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Resource
    private IAdminService adminService;

    @GetMapping
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(required = false) String phone) {
        return Result.ok(adminService.queryUsers(current, phone));
    }
}
