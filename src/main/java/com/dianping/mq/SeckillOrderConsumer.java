package com.dianping.mq;

import cn.hutool.core.bean.BeanUtil;
import com.dianping.entity.VoucherOrder;
import com.dianping.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = "seckill.order.queue")
    public void handleSeckillOrder(Map<String, Object> message) {
        try {
            VoucherOrder order = BeanUtil.toBean(message, VoucherOrder.class);
            log.debug("Consuming seckill order: {}", order.getId());
            voucherOrderService.createOrder(order);
        } catch (Exception e) {
            log.error("Failed to process seckill order", e);
        }
    }
}
