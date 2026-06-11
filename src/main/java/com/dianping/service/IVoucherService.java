package com.dianping.service;

import com.dianping.common.Result;
import com.dianping.entity.Voucher;
import java.util.List;

public interface IVoucherService {
    void addVoucher(Voucher voucher);
    Result addSeckillVoucher(Voucher voucher, Integer stock, java.time.LocalDateTime beginTime, java.time.LocalDateTime endTime);
    List<Voucher> queryByShopId(Long shopId);
}
