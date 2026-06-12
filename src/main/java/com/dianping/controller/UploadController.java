package com.dianping.controller;

import cn.hutool.core.util.StrUtil;
import com.dianping.common.ErrorCode;
import com.dianping.common.BusinessException;
import com.dianping.common.Result;
import com.dianping.utils.MinIOUploadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Resource
    private MinIOUploadUtil minIOUploadUtil;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp");

    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String originalName = file.getOriginalFilename();
        if (StrUtil.isNotBlank(originalName)) {
            String suffix = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_TYPES.contains(suffix)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_ERROR);
            }
        }
        String url = minIOUploadUtil.upload(file, "blog");
        return Result.ok(url);
    }
}
