#!/bin/bash
# 使用 Docker 启动 Nginx 反向代理（负载均衡到 8081 / 8082）

NGINX_CONF="/home/zlwu/agent_code/dianping/deploy/nginx.conf"

echo "Starting Nginx (Docker) for dianping cluster..."
echo "  Backend: 8081, 8082"
echo "  Listen:  localhost:80"

docker rm -f dianping-nginx 2>/dev/null

docker run -d --name dianping-nginx \
  --net=host \
  -v "$NGINX_CONF":/etc/nginx/conf.d/default.conf:ro \
  nginx:alpine

echo ""
echo "Nginx started! Access: http://localhost"
echo "Docker container name: dianping-nginx"
