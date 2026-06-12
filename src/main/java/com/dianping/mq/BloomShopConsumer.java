package com.dianping.mq;

import com.dianping.service.impl.BloomFilterService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class BloomShopConsumer {

    @Resource
    private BloomFilterService bloomFilterService;

    @RabbitListener(queues = "#{bloomShopQueue.name}", ackMode = "MANUAL")
    public void handleShopId(Long shopId, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            bloomFilterService.addShopId(shopId);
            log.debug("BloomFilter synced shopId: {}", shopId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to sync BloomFilter for shop {}, will requeue", shopId, e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ignored) {
            }
        }
    }
}
