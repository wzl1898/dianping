package com.dianping.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Shop;
import com.dianping.service.IAdminService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/admin/shop")
public class AdminShopController {

    @Resource
    private IAdminService adminService;

    @GetMapping
    public Result<Page<Shop>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(required = false) String name) {
        return Result.ok(adminService.queryShops(current, name));
    }

    @PostMapping
    public Result save(@RequestBody Shop shop) {
        return adminService.saveShop(shop);
    }

    @PutMapping
    public Result update(@RequestBody Shop shop) {
        return adminService.updateShop(shop);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return adminService.deleteShop(id);
    }
}
