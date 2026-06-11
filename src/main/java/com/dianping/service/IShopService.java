package com.dianping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Shop;
import com.dianping.vo.ShopVO;
import java.util.List;

public interface IShopService {
    Shop queryById(Long id);
    Page<Shop> queryByType(Long typeId, Integer current, Double x, Double y);
    Result update(Shop shop);
    List<ShopVO> queryNearby(Long typeId, Double x, Double y, Double distance, Integer current);
}
