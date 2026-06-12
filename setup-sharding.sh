#!/bin/bash
# ============================================================
# 分库分表环境初始化脚本
# 前提：MySQL 已在 localhost:3307 运行（由 setup-mysql.sh 启动）
# ============================================================

MYSQL_USER="root"
MYSQL_PASS="root"
MYSQL_HOST="localhost"
MYSQL_PORT="3307"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

MYSQL_CMD="mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} -P${MYSQL_PORT}"

echo "=========================================="
echo "  创建分片数据库 ds0 + ds1"
echo "=========================================="

# 执行 DDL 创建分片数据库和表
${MYSQL_CMD} < "${SCRIPT_DIR}/sql/ddl-sharding.sql"

if [ $? -eq 0 ]; then
    echo "✅ 分片数据库创建成功！"
    echo "   - dianping_ds0（完整数据源，所有表 + tb_voucher_order 的 user_id%2=0 分片）"
    echo "   - dianping_ds1（仅 tb_voucher_order 的 user_id%2=1 分片）"
else
    echo "❌ 创建失败，请检查 MySQL 连接"
    exit 1
fi

echo ""
echo "=========================================="
echo "  验证表结构"
echo "=========================================="

echo "--- ds0 表列表 ---"
${MYSQL_CMD} -e "USE dianping_ds0; SHOW TABLES;"

echo ""
echo "--- ds1 表列表 ---"
${MYSQL_CMD} -e "USE dianping_ds1; SHOW TABLES;"

echo ""
echo "=========================================="
echo "  环境就绪！启动方式："
echo "=========================================="
echo "  # 非分片模式（默认）"
echo "  ./start.sh"
echo ""
echo "  # 分片模式"
echo "  ./start.sh --spring.profiles.active=sharding"
echo ""
echo "  # 或使用 Maven"
echo "  mvn spring-boot:run -Dspring-boot.run.profiles=sharding"
echo "=========================================="
