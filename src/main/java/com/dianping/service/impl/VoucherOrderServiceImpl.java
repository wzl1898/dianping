package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.SeckillVoucherMapper;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class VoucherOrderServiceImpl implements IVoucherOrderService {

    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }

        // 1. Check time range from DB
        SeckillVoucher sv = seckillVoucherMapper.selectById(voucherId);
        if (sv == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sv.getBeginTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(sv.getEndTime())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }

        // 2. Execute Lua script
        String stockKey = "seckill:stock:" + voucherId;
        String orderKey = "seckill:order:" + voucherId;
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Arrays.asList(stockKey, orderKey),
                userId.toString()
        );

        if (result == null || result != 0) {
            if (result == 1) {
                throw new BusinessException(ErrorCode.SECKILL_STOCK_INSUFFICIENT);
            }
            throw new BusinessException(ErrorCode.SECKILL_REPEAT_ORDER);
        }

        // 3. Create order and send to MQ
        long orderId = redisIdWorker.nextId("seckill");
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        order.setStatus(1);

        rabbitTemplate.convertAndSend(
                "seckill.direct",
                "seckill.order",
                BeanUtil.beanToMap(order, false, true));

        return Result.ok(orderId);
    }

    @Override
    @Transactional
    public void createOrder(VoucherOrder order) {
        // Idempotency: unique key (userId, voucherId) will prevent duplicates
        try {
            voucherOrderMapper.insert(order);
            log.debug("Order created: {}", order.getId());
        } catch (Exception e) {
            log.warn("Duplicate order ignored: {}", order.getId(), e);
        }
    }

    @Override
    public VoucherOrder queryById(Long orderId) {
        return voucherOrderMapper.selectById(orderId);
    }
}
