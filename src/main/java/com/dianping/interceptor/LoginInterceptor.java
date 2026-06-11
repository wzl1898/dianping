package com.dianping.interceptor;

import com.dianping.common.ErrorCode;
import com.dianping.common.Result;
import com.dianping.utils.UserHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser() == null) {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(401);
            response.getWriter().write(MAPPER.writeValueAsString(Result.error(ErrorCode.USER_NOT_LOGIN)));
            return false;
        }
        return true;
    }
}
