#!/bin/bash
# 环境准备脚本 - 一键启动依赖服务（Docker 版本）
# 如果使用本地安装的服务，请跳过此脚本

echo "启动依赖服务..."
docker run -d --name redis -p 6379:6379 redis:7-alpine 2>/dev/null || echo "redis 已运行"
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management 2>/dev/null || echo "rabbitmq 已运行"
docker run -d --name minio -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address :9001 2>/dev/null || echo "minio 已运行"
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=dianping \
  mysql:8 2>/dev/null || echo "mysql 已运行"

echo "所有服务已启动"
echo "Redis:      localhost:6379"
echo "RabbitMQ:   localhost:5672 (管理台: http://localhost:15672)"
echo "MinIO:      http://localhost:9000 (控制台: http://localhost:9001)"
echo "MySQL:      localhost:3306 (root/root)"
