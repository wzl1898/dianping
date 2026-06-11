package com.dianping.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
public class LoginFormDTO {
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
    private String code;
    private String password;
}
