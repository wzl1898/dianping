package com.dianping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.entity.Shop;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ShopMapper extends BaseMapper<Shop> {

    /**
     * 查询所有商户 ID，用于布隆过滤器初始化
     */
    @Select("SELECT id FROM tb_shop")
    List<Long> selectAllIds();
}
