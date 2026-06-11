package com.dianping.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MinIOConfig {

    @Bean
    public MinioClient minioClient(MinIOProperties properties) throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();

        for (Map.Entry<String, String> entry : properties.getBucket().entrySet()) {
            String bucketName = entry.getValue();
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                // Set public read policy
                String policy = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [{\n" +
                        "    \"Effect\": \"Allow\",\n" +
                        "    \"Principal\": {\"AWS\": [\"*\"]},\n" +
                        "    \"Action\": [\"s3:GetObject\"],\n" +
                        "    \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                        "  }]\n" +
                        "}";
                client.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build());
            }
        }
        return client;
    }
}
