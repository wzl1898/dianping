package com.dianping.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Voucher;
import com.dianping.service.IAdminService;
import com.dianping.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/voucher")
public class AdminVoucherController {

    @Resource
    private IAdminService adminService;

    @Resource
    private IVoucherService voucherService;

    @GetMapping
    public Result<Page<Voucher>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(required = false) Long shopId) {
        return Result.ok(adminService.queryVouchers(current, shopId));
    }

    @PostMapping
    public Result save(@RequestBody Map<String, Object> params) {
        Voucher voucher = new Voucher();
        voucher.setShopId(Long.valueOf(params.get("shopId").toString()));
        voucher.setTitle((String) params.get("title"));
        voucher.setSubTitle((String) params.getOrDefault("subTitle", ""));
        voucher.setRules((String) params.getOrDefault("rules", ""));
        voucher.setPayValue(Long.valueOf(params.get("payValue").toString()));
        voucher.setActualValue(Long.valueOf(params.get("actualValue").toString()));
        voucher.setType(Integer.valueOf(params.getOrDefault("type", "0").toString()));

        if (voucher.getType() == 1) {
            Integer stock = Integer.valueOf(params.get("stock").toString());
            LocalDateTime beginTime = LocalDateTime.parse(
                    params.get("beginTime").toString().substring(0, 19));
            LocalDateTime endTime = LocalDateTime.parse(
                    params.get("endTime").toString().substring(0, 19));
            return voucherService.addSeckillVoucher(voucher, stock, beginTime, endTime);
        } else {
            voucherService.addVoucher(voucher);
            return Result.ok(voucher.getId());
        }
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return adminService.deleteVoucher(id);
    }
}
