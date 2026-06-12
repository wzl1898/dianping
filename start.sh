#!/bin/bash
# 项目启动脚本
# 前置条件: Java 8+, Maven 3.6+, MySQL 8+, Redis, RabbitMQ, MinIO

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "================================================"
echo "  高并发本地生活服务平台 - 启动脚本"
echo "================================================"

# 1. 检查依赖
echo "[1/5] 检查环境..."
command -v java >/dev/null 2>&1 || { echo "需要 Java 8+"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "需要 Maven 3.6+"; exit 1; }
echo "  ✓ Java: $(java -version 2>&1 | head -1)"
echo "  ✓ Maven: $(mvn --version 2>&1 | head -1)"

# 2. 初始化数据库
echo "[2/5] 初始化数据库..."
if command -v mysql >/dev/null 2>&1; then
    echo "  执行 sql/ddl.sql..."
    mysql -u root -proot < "$PROJECT_DIR/sql/ddl.sql" 2>/dev/null && echo "  ✓ 数据库初始化完成" || echo "  ⚠ 数据库初始化失败，请手动执行 sql/ddl.sql"
else
    echo "  ⚠ 未找到 mysql 客户端，请手动执行 sql/ddl.sql"
fi

# 3. 编译项目
echo "[3/5] 编译项目..."
cd "$PROJECT_DIR"
mvn clean compile -q || { echo "编译失败"; exit 1; }
echo "  ✓ 编译成功"

# 4. 运行测试
echo "[4/5] 运行测试..."
mvn test -q || echo "  ⚠ 部分测试失败，请检查"

# 5. 启动应用
echo "[5/5] 启动应用..."
echo "  应用将在 http://localhost:8080 启动"
echo "  API 文档: http://localhost:8080/swagger-ui.html"
if [ "$1" == "--sharding" ]; then
    echo "  ★ 分库分表模式 [spring.profiles.active=sharding]"
    PROFILES="--spring-boot.run.profiles=sharding"
else
    PROFILES=""
fi
mvn spring-boot:run ${PROFILES}
