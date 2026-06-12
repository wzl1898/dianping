package com.dianping.controller;

import com.dianping.annotation.RateLimiter;
import com.dianping.common.Result;
import com.dianping.entity.VoucherOrder;
import com.dianping.service.IVoucherOrderService;
import com.dianping.service.IVoucherService;
import com.dianping.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @GetMapping("/list/{shopId}")
    public Result list(@PathVariable Long shopId) {
        return Result.ok(voucherService.queryByShopId(shopId));
    }

    @RateLimiter(key = "seckill", time = 5, count = 3, limitType = RateLimiter.LimitType.USER)
    @PostMapping("/seckill/{voucherId}")
    public Result seckill(@PathVariable Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @GetMapping("/order/{orderId}")
    public Result getOrder(@PathVariable Long orderId) {
        VoucherOrder order = voucherOrderService.queryById(orderId);
        return order != null ? Result.ok(order) : Result.error("订单不存在");
    }
}
