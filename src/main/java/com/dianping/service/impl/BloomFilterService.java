package com.dianping.service.impl;

import com.dianping.mapper.ShopMapper;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 基于 Guava BloomFilter 的本地布隆过滤器服务（JVM 内存）
 * <p>
 * 用于缓存穿透防护：查询 DB 前先过布隆过滤器，
 * 若判定不存在则直接返回，避免无效查询打到数据库。
 * <p>
 * 多实例通过 MQ 同步新增的 ID，启动时从 DB 全量重建。
 */
@Slf4j
@Service
public class BloomFilterService {

    /** 预期商户数 */
    private static final long EXPECTED_INSERTIONS = 10_000L;

    /** 期望假阳性率 1% */
    private static final double FALSE_POSITIVE_RATE = 0.01;

    @Resource
    private ShopMapper shopMapper;

    /** 本地 JVM 布隆过滤器 */
    private final BloomFilter<Long> bloomFilter;

    public BloomFilterService() {
        this.bloomFilter = BloomFilter.create(
                Funnels.longFunnel(), EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE);
        log.info("BloomFilter created: expectedInsertions={}, fpp={}",
                EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE);
    }

    /**
     * 应用启动完成后，从数据库加载已有商户 ID 初始化布隆过滤器
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Starting BloomFilter initialization - loading shop IDs from DB...");
        try {
            List<Long> allShopIds = shopMapper.selectAllIds();
            if (allShopIds != null && !allShopIds.isEmpty()) {
                for (Long id : allShopIds) {
                    bloomFilter.put(id);
                }
                log.info("BloomFilter initialized with {} shop IDs", allShopIds.size());
            } else {
                log.warn("No shop IDs found in DB, BloomFilter will be empty");
            }
        } catch (Exception e) {
            log.error("Failed to initialize BloomFilter", e);
        }
    }

    /**
     * 判断商户 ID 是否可能存在
     *
     * @return false = 一定不存在；true = 可能存在（有假阳性）
     */
    public boolean mightContainShopId(Long shopId) {
        return bloomFilter.mightContain(shopId);
    }

    /**
     * 向布隆过滤器中添加商户 ID
     */
    public void addShopId(Long shopId) {
        bloomFilter.put(shopId);
    }
}
