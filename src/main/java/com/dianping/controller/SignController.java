package com.dianping.controller;

import com.dianping.common.Result;
import com.dianping.service.ISignService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/sign")
public class SignController {

    @Resource
    private ISignService signService;

    @PostMapping
    public Result sign() {
        return signService.sign();
    }

    @GetMapping("/count")
    public Result count() {
        return signService.signCount();
    }
}
