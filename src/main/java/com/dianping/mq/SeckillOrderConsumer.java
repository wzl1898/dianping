package com.dianping.mq;

import cn.hutool.core.bean.BeanUtil;
import com.dianping.entity.VoucherOrder;
import com.dianping.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = "seckill.order.queue", ackMode = "MANUAL")
    public void handleSeckillOrder(Map<String, Object> message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            VoucherOrder order = BeanUtil.toBean(message, VoucherOrder.class);
            log.debug("Processing seckill order: {}", order.getId());
            voucherOrderService.createOrder(order);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to process seckill order, sending to DLQ", e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ignored) {
            }
        }
    }
}
