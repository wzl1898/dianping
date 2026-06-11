package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.service.IShopService;
import com.dianping.vo.ShopVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShopServiceImpl implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private ObjectMapper jsonMapper;

    private static final String CACHE_SHOP_KEY = "cache:shop:";
    private static final String LOCK_SHOP_KEY = "lock:shop:";
    private static final long CACHE_TTL = 30L;
    private static final long NULL_TTL = 5L;
    private static final long LOCK_TTL = 10L;

    @Override
    public Shop queryById(Long id) {
        // 互斥锁方案解决缓存击穿
        String cacheKey = CACHE_SHOP_KEY + id;
        String cacheVal = stringRedisTemplate.opsForValue().get(cacheKey);

        if (StrUtil.isNotBlank(cacheVal)) {
            try {
                return jsonMapper.readValue(cacheVal, Shop.class);
            } catch (JsonProcessingException e) {
                log.warn("Cache deserialize failed", e);
            }
        }

        // 缓存空对象穿透处理
        if (cacheVal != null && cacheVal.isEmpty()) {
            return null;
        }

        // 互斥锁重建缓存
        Shop shop = null;
        String lockKey = LOCK_SHOP_KEY + id;
        try {
            boolean locked = tryLock(lockKey);
            if (!locked) {
                Thread.sleep(50);
                return queryById(id);
            }
            // 双重检查
            cacheVal = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cacheVal)) {
                return jsonMapper.readValue(cacheVal, Shop.class);
            }
            shop = shopMapper.selectById(id);
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, "", NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            String json = jsonMapper.writeValueAsString(shop);
            long ttl = CACHE_TTL + (long) (Math.random() * 30);
            stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Cache rebuild failed", e);
        } finally {
            unlock(lockKey);
        }
        return shop;
    }

    @Override
    public Page<Shop> queryByType(Long typeId, Integer current, Double x, Double y) {
        Page<Shop> page = shopMapper.selectPage(
                new Page<>(current, 10),
                new LambdaQueryWrapper<Shop>().eq(Shop::getTypeId, typeId));
        return page;
    }

    @Override
    public List<ShopVO> queryNearby(Long typeId, Double x, Double y, Double distance, Integer current) {
        // Redis GEO search
        String geoKey = "shop:geo:" + typeId;
        try {
            Circle circle = new Circle(new Point(x, y), new Distance(distance / 1000.0, Metrics.KILOMETERS));
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs().includeDistance().sortAscending();
            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                    stringRedisTemplate.opsForGeo().radius(geoKey, circle, args);
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> results = geoResults.getContent();
            List<ShopVO> list = new ArrayList<>();
            if (results != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
                    String shopIdStr = result.getContent().getName();
                    Shop shop = shopMapper.selectById(Long.valueOf(shopIdStr));
                    if (shop != null) {
                        ShopVO vo = BeanUtil.copyProperties(shop, ShopVO.class);
                        double distKm = result.getDistance().getValue();
                        vo.setDistance(distKm < 1 ? String.format("%.0fm", distKm * 1000)
                                : String.format("%.1fkm", distKm));
                        list.add(vo);
                    }
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("GEO search fallback to DB: {}", e.getMessage());
            List<Shop> shops = shopMapper.selectList(
                    new LambdaQueryWrapper<Shop>().eq(Shop::getTypeId, typeId));
            return BeanUtil.copyToList(shops, ShopVO.class);
        }
    }

    @Override
    public Result update(Shop shop) {
        shopMapper.updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok(null);
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
