package com.dianping.controller;

import com.dianping.common.Result;
import com.dianping.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/shop-type")
public class ShopTypeController {

    @Resource
    private IShopTypeService shopTypeService;

    @GetMapping("/list")
    public Result list() {
        return Result.ok(shopTypeService.queryAllTypes());
    }
}
