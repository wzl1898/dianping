#!/bin/bash
# 启动双应用实例（8081 / 8082）

JAR_PATH="/home/zlwu/agent_code/dianping/target/dianping-1.0.0.jar"
JAVA_CMD="/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"

echo "Starting dianping instance 1 (port 8081)..."
nohup $JAVA_CMD -jar $JAR_PATH --server.port=8081 > /tmp/dianping-app-8081.log 2>&1 &
echo "  PID: $!"
echo "  Log: /tmp/dianping-app-8081.log"

echo "Starting dianping instance 2 (port 8082)..."
nohup $JAVA_CMD -jar $JAR_PATH --server.port=8082 > /tmp/dianping-app-8082.log 2>&1 &
echo "  PID: $!"
echo "  Log: /tmp/dianping-app-8082.log"

echo ""
echo "Both instances started. Use 'deploy/start-nginx.sh' to start Nginx."
echo "Or access directly: http://localhost:8081 | http://localhost:8082"
