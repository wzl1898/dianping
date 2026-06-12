package com.dianping.service;

import com.dianping.common.Result;

public interface IUvService {
    void addUv(Long userId);
    Result getUv(String date);
}
