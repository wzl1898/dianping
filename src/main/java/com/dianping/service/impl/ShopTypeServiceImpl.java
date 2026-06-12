package com.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dianping.entity.ShopType;
import com.dianping.mapper.ShopTypeMapper;
import com.dianping.service.IShopTypeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShopTypeServiceImpl implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopTypeMapper shopTypeMapper;

    @Resource
    private ObjectMapper mapper;

    @Resource(name = "shopTypeCache")
    private Cache<String, List<ShopType>> shopTypeCache;

    private static final String CACHE_KEY = "cache:shop:type";

    @Override
    public List<ShopType> queryAllTypes() {
        // 1. Caffeine L1
        List<ShopType> cached = shopTypeCache.getIfPresent(CACHE_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2. Redis L2
        String cacheVal = stringRedisTemplate.opsForValue().get(CACHE_KEY);
        if (StrUtil.isNotBlank(cacheVal)) {
            try {
                List<ShopType> types = mapper.readValue(cacheVal,
                        mapper.getTypeFactory().constructCollectionType(List.class, ShopType.class));
                shopTypeCache.put(CACHE_KEY, types);
                return types;
            } catch (JsonProcessingException e) {
                log.warn("Cache deserialize failed", e);
            }
        }

        // 3. DB
        List<ShopType> types = shopTypeMapper.selectList(
                new LambdaQueryWrapper<ShopType>().orderByAsc(ShopType::getSort));
        try {
            String json = mapper.writeValueAsString(types);
            stringRedisTemplate.opsForValue().set(CACHE_KEY, json, 30L, TimeUnit.MINUTES);
            shopTypeCache.put(CACHE_KEY, types);
        } catch (JsonProcessingException e) {
            log.error("Cache serialize failed", e);
        }
        return types;
    }
}
