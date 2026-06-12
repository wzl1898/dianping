package com.dianping.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String SECKILL_EXCHANGE = "seckill.direct";
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    // ========== 秒杀死信队列 ==========
    public static final String SECKILL_DLX = "seckill.dlx";
    public static final String SECKILL_DLQ = "seckill.dlq";

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_DLX)
                .withArgument("x-dead-letter-routing-key", SECKILL_DLQ)
                .build();
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public Queue seckillDlq() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public DirectExchange seckillDlx() {
        return new DirectExchange(SECKILL_DLX);
    }

    @Bean
    public Binding seckillDlqBinding() {
        return BindingBuilder.bind(seckillDlq())
                .to(seckillDlx())
                .with(SECKILL_DLQ);
    }

    // ========== 布隆过滤器同步（Fanout 广播） ==========

    public static final String BLOOM_FANOUT = "bloom.shop.fanout";
    public static final String BLOOM_ROUTING_KEY = "bloom.shop.sync";

    @Bean
    public Queue bloomShopQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public FanoutExchange bloomExchange() {
        return new FanoutExchange(BLOOM_FANOUT);
    }

    @Bean
    public Binding bloomBinding() {
        return BindingBuilder.bind(bloomShopQueue())
                .to(bloomExchange());
    }

    // ========== 缓存失效广播（Fanout 广播） ==========

    public static final String CACHE_INVALIDATE_FANOUT = "cache.invalidate.fanout";
    public static final String CACHE_INVALIDATE_ROUTING_KEY = "cache.invalidate.shop";

    @Bean
    public Queue cacheInvalidateQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public FanoutExchange cacheInvalidateExchange() {
        return new FanoutExchange(CACHE_INVALIDATE_FANOUT);
    }

    @Bean
    public Binding cacheInvalidateBinding() {
        return BindingBuilder.bind(cacheInvalidateQueue())
                .to(cacheInvalidateExchange());
    }
}
