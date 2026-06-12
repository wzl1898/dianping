package com.dianping.mq;

import com.dianping.entity.Shop;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class CacheInvalidateConsumer {

    @Resource(name = "shopCache")
    private Cache<Long, Shop> shopCache;

    @RabbitListener(queues = "#{cacheInvalidateQueue.name}")
    public void handleShopInvalidate(Long shopId) {
        shopCache.invalidate(shopId);
        log.debug("Caffeine cache invalidated for shop: {}", shopId);
    }
}
