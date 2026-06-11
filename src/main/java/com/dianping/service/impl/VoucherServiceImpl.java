package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.Voucher;
import com.dianping.mapper.SeckillVoucherMapper;
import com.dianping.mapper.VoucherMapper;
import com.dianping.service.IVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VoucherServiceImpl implements IVoucherService {

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void addVoucher(Voucher voucher) {
        voucherMapper.insert(voucher);
    }

    @Override
    @Transactional
    public Result addSeckillVoucher(Voucher voucher, Integer stock,
                                    LocalDateTime beginTime, LocalDateTime endTime) {
        voucher.setType(1);
        voucherMapper.insert(voucher);

        SeckillVoucher sv = new SeckillVoucher();
        sv.setVoucherId(voucher.getId());
        sv.setStock(stock);
        sv.setBeginTime(beginTime);
        sv.setEndTime(endTime);
        seckillVoucherMapper.insert(sv);

        // Save stock to Redis
        stringRedisTemplate.opsForValue().set(
                "seckill:stock:" + voucher.getId(),
                String.valueOf(stock));
        return Result.ok(voucher.getId());
    }

    @Override
    public List<Voucher> queryByShopId(Long shopId) {
        List<Voucher> vouchers = voucherMapper.selectList(
                new LambdaQueryWrapper<Voucher>()
                        .eq(Voucher::getShopId, shopId)
                        .eq(Voucher::getStatus, 1));
        for (Voucher v : vouchers) {
            if (v.getType() == 1) {
                SeckillVoucher sv = seckillVoucherMapper.selectById(v.getId());
                if (sv != null) {
                    v.setSubTitle(sv.getBeginTime() + "~" + sv.getEndTime());
                }
            }
        }
        return vouchers;
    }
}
