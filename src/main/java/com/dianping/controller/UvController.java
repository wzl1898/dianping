package com.dianping.controller;

import com.dianping.common.Result;
import com.dianping.service.IUvService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/uv")
public class UvController {

    @Resource
    private IUvService uvService;

    @GetMapping
    public Result getUv(@RequestParam String date) {
        return uvService.getUv(date);
    }
}
