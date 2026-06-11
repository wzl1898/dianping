package com.dianping.utils;

import cn.hutool.core.util.StrUtil;
import com.dianping.config.MinIOProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@Component
public class MinIOUploadUtil {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinIOProperties minIOProperties;

    public String upload(MultipartFile file, String bucketKey) {
        try {
            String bucket = minIOProperties.getBucket().get(bucketKey);
            if (StrUtil.isBlank(bucket)) {
                throw new RuntimeException("Bucket not found for key: " + bucketKey);
            }
            String originalName = file.getOriginalFilename();
            String suffix = "";
            if (StrUtil.isNotBlank(originalName) && originalName.contains(".")) {
                suffix = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectName = UUID.randomUUID().toString().replace("-", "") + suffix;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String url = minIOProperties.getEndpoint() + "/" + bucket + "/" + objectName;
            log.info("MinIO upload success: {}", url);
            return url;
        } catch (Exception e) {
            log.error("MinIO upload failed", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
