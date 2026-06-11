package com.dianping.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(10000, "成功"),
    UNKNOWN_ERROR(10001, "未知错误"),
    PARAM_ERROR(10002, "参数错误"),

    USER_NOT_LOGIN(10100, "未登录"),
    INVALID_CODE(10101, "验证码错误"),
    CODE_EXPIRED(10102, "验证码已过期"),
    CODE_TOO_FREQUENT(10103, "发送过于频繁"),
    INVALID_PHONE(10104, "手机号格式错误"),

    SECKILL_STOCK_INSUFFICIENT(10200, "库存不足"),
    SECKILL_REPEAT_ORDER(10201, "重复下单"),
    SECKILL_NOT_STARTED(10202, "秒杀未开始"),
    SECKILL_ENDED(10203, "秒杀已结束"),
    SECKILL_BUSY(10204, "秒杀太火爆，请稍后再试"),
    RATE_LIMITED(10205, "请求过于频繁，请稍后再试"),

    ORDER_NOT_FOUND(10300, "订单不存在"),

    ADMIN_AUTH_FAIL(10400, "管理员认证失败"),

    FILE_TYPE_ERROR(10500, "不支持的文件类型"),
    FILE_TOO_LARGE(10501, "文件过大");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
