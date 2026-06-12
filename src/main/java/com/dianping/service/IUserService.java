package com.dianping.service;

import com.dianping.common.Result;
import com.dianping.dto.LoginFormDTO;

public interface IUserService {
    Result sendCode(String phone);
    Result login(LoginFormDTO form);
    Result me();
    Result logout();
}
