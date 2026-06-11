package com.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.entity.Shop;
import com.dianping.entity.ShopType;
import com.dianping.mapper.ShopMapper;
import com.dianping.mapper.ShopTypeMapper;
import com.dianping.service.IShopTypeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Override
    public List<ShopType> queryAllTypes() {
        String cacheKey = "cache:shop:type";
        String cacheVal = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cacheVal)) {
            try {
                return mapper.readValue(cacheVal,
                        mapper.getTypeFactory().constructCollectionType(List.class, ShopType.class));
            } catch (JsonProcessingException e) {
                log.warn("Cache deserialize failed", e);
            }
        }
        List<ShopType> types = shopTypeMapper.selectList(
                new LambdaQueryWrapper<ShopType>().orderByAsc(ShopType::getSort));
        try {
            String json = mapper.writeValueAsString(types);
            stringRedisTemplate.opsForValue().set(cacheKey, json, 30L, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Cache serialize failed", e);
        }
        return types;
    }
}
