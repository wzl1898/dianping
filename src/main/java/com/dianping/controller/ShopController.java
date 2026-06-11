package com.dianping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Shop;
import com.dianping.service.IShopService;
import com.dianping.vo.ShopVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Resource
    private IShopService shopService;

    @GetMapping("/{id}")
    public Result<Shop> getById(@PathVariable Long id) {
        Shop shop = shopService.queryById(id);
        return Result.ok(shop);
    }

    @GetMapping("/list")
    public Result<Page<Shop>> list(
            @RequestParam(value = "typeId", required = false) Long typeId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(required = false) Double x,
            @RequestParam(required = false) Double y) {
        if (typeId == null) {
            return Result.ok(shopService.queryByType(null, current, x, y));
        }
        return Result.ok(shopService.queryByType(typeId, current, x, y));
    }

    @GetMapping("/list/nearby")
    public Result<List<ShopVO>> nearby(
            @RequestParam(defaultValue = "1") Long typeId,
            @RequestParam Double x,
            @RequestParam Double y,
            @RequestParam(defaultValue = "5000") Double distance,
            @RequestParam(defaultValue = "1") Integer current) {
        return Result.ok(shopService.queryNearby(typeId, x, y, distance, current));
    }
}
