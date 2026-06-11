package com.dianping.service;

import com.dianping.common.Result;
import com.dianping.entity.VoucherOrder;

public interface IVoucherOrderService {
    Result seckillVoucher(Long voucherId);
    void createOrder(VoucherOrder order);
    VoucherOrder queryById(Long orderId);
}
