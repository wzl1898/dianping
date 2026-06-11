package com.dianping.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    String key() default "rate:limit";
    int time() default 5;
    int count() default 5;
    LimitType limitType() default LimitType.IP;

    enum LimitType {
        IP, USER
    }
}
