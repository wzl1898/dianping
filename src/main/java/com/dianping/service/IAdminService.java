package com.dianping.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.Result;
import com.dianping.entity.Admin;
import com.dianping.entity.Shop;
import com.dianping.entity.User;
import com.dianping.entity.Voucher;

public interface IAdminService {
    Result login(String username, String password);
    Result logout(String token);
    Page<Shop> queryShops(Integer current, String name);
    Result saveShop(Shop shop);
    Result updateShop(Shop shop);
    Result deleteShop(Long id);
    Page<Voucher> queryVouchers(Integer current, Long shopId);
    Result deleteVoucher(Long id);
    Page<User> queryUsers(Integer current, String phone);
    Admin getCurrentAdmin(String token);
}
