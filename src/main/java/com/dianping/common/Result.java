package com.dianping.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Boolean success;
    private T data;
    private String errorMsg;
    private Integer total;

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, data, null, null);
    }

    public static <T> Result<T> ok(T data, Integer total) {
        return new Result<>(true, data, null, total);
    }

    public static <T> Result<T> error(String errorMsg) {
        return new Result<>(false, null, errorMsg, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(false, null, errorCode.getMessage(), null);
    }
}
