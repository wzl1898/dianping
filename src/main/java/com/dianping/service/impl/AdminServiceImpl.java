package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.entity.Admin;
import com.dianping.entity.Shop;
import com.dianping.entity.User;
import com.dianping.entity.Voucher;
import com.dianping.mapper.*;
import com.dianping.service.IAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AdminServiceImpl implements IAdminService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private ShopMapper shopMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result login(String username, String password) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username));
        if (admin == null || !password.equals(admin.getPassword())) {
            // For learning project, plaintext compare
            throw new BusinessException(ErrorCode.ADMIN_AUTH_FAIL);
        }
        String token = "admin:" + UUID.randomUUID().toString();
        Map<String, String> adminMap = new HashMap<>();
        adminMap.put("id", admin.getId().toString());
        adminMap.put("username", admin.getUsername());
        adminMap.put("nickname", admin.getNickname());
        stringRedisTemplate.opsForHash().putAll("admin:token:" + token, adminMap);
        stringRedisTemplate.expire("admin:token:" + token, 60, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result logout(String token) {
        stringRedisTemplate.delete("admin:token:" + token);
        return Result.ok(null);
    }

    @Override
    public Page<Shop> queryShops(Integer current, String name) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(Shop::getName, name);
        }
        return shopMapper.selectPage(new Page<>(current, 20), wrapper);
    }

    @Override
    public Result saveShop(Shop shop) {
        shopMapper.insert(shop);
        // Add to GEO
        String geoKey = "shop:geo:" + shop.getTypeId();
        stringRedisTemplate.opsForGeo().add(geoKey,
                new org.springframework.data.geo.Point(shop.getX(), shop.getY()),
                shop.getId().toString());
        return Result.ok(shop.getId());
    }

    @Override
    public Result updateShop(Shop shop) {
        shopMapper.updateById(shop);
        stringRedisTemplate.delete("cache:shop:" + shop.getId());
        // Update GEO
        String geoKey = "shop:geo:" + shop.getTypeId();
        stringRedisTemplate.opsForGeo().add(geoKey,
                new org.springframework.data.geo.Point(shop.getX(), shop.getY()),
                shop.getId().toString());
        return Result.ok(null);
    }

    @Override
    public Result deleteShop(Long id) {
        shopMapper.deleteById(id);
        stringRedisTemplate.delete("cache:shop:" + id);
        return Result.ok(null);
    }

    @Override
    public Page<Voucher> queryVouchers(Integer current, Long shopId) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        if (shopId != null) {
            wrapper.eq(Voucher::getShopId, shopId);
        }
        return voucherMapper.selectPage(new Page<>(current, 20), wrapper);
    }

    @Override
    public Result deleteVoucher(Long id) {
        voucherMapper.deleteById(id);
        return Result.ok(null);
    }

    @Override
    public Page<User> queryUsers(Integer current, String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(phone)) {
            wrapper.like(User::getPhone, phone);
        }
        return userMapper.selectPage(new Page<>(current, 20), wrapper);
    }

    @Override
    public Admin getCurrentAdmin(String token) {
        String key = "admin:token:" + token;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return null;
        }
        Admin admin = new Admin();
        admin.setId(Long.valueOf((String) entries.get("id")));
        admin.setUsername((String) entries.get("username"));
        admin.setNickname((String) entries.get("nickname"));
        return admin;
    }
}
