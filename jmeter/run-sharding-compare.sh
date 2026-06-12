#!/bin/bash
# ============================================================
# 分库分表效果对比测试脚本
# 分别运行 "分片模式" 和 "非分片模式" 并对比结果
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="${PROJECT_DIR}/jmeter/report"
JMETER="${JMETER_HOME}/bin/jmeter"
if [ -z "${JMETER_HOME}" ]; then
    JMETER="jmeter"
fi

# 测试参数
HOST="localhost"
PORT="8080"
VOUCHER_ID="1"
TEST_DATA_SCRIPT="${PROJECT_DIR}/jmeter/scenarios/gen-sharding-test-data.sh"
TEST_PLAN="${PROJECT_DIR}/jmeter/scenarios/PT-8-sharding-compare.jmx"

echo "============================================="
echo "  分库分表效果对比测试"
echo "============================================="
echo ""
echo "测试说明："
echo "  1. 先启动非分片模式应用（不用 sharding profile）"
echo "  2. 运行 JMeter 测试，记录基线"
echo "  3. 切换到分片模式（用 sharding profile）"
echo "  4. 再次运行 JMeter 测试，记录分片结果"
echo "  5. 对比两次报告的 TPS、P50、P95、P99"
echo ""
echo "前提条件："
echo "  - MySQL 已运行（localhost:3307）"
echo "  - Redis 已运行（localhost:6380）"
echo "  - RabbitMQ 已运行（localhost:5672）"
echo "  - JMeter 已安装（jmeter 命令可用）"
echo ""

# 检查依赖
for cmd in java mvn mysql jmeter; do
    if ! command -v ${cmd} &>/dev/null; then
        echo "❌ 找不到 ${cmd}，请先安装"
        exit 1
    fi
done

echo "✅ 环境检查通过"

# 准备测试数据
echo ""
echo "============================================="
echo "  准备测试数据"
echo "============================================="
bash "${TEST_DATA_SCRIPT}"

# 编译项目
echo ""
echo "============================================="
echo "  编译项目"
echo "============================================="
cd "${PROJECT_DIR}"
mvn clean package -DskipTests -q
echo "✅ 编译成功"

# =============================================
# 测试 1：非分片模式（基线）
# =============================================
echo ""
echo "============================================="
echo "  测试 1/2：非分片模式（基线）"
echo "============================================="
echo "  步骤："
echo "    1. 启动应用（无 profile）"
echo "    2. 等待启动完成（30s）"
echo "    3. 运行 JMeter 测试"
echo "    4. 停止应用"
echo ""
read -p "  准备好后按 Enter 开始..."

BASE_REPORT="${REPORT_DIR}/baseline"
mkdir -p "${BASE_REPORT}"

echo "[启动] 非分片模式..."
nohup java -jar target/dianping-1.0.0.jar \
    --server.port=${PORT} \
    > /tmp/sharding-test-nosharding.log 2>&1 &
NOSHARD_PID=$!

echo "[等待] 应用启动（40s）..."
sleep 40

echo "[测试] 运行 JMeter..."
jmeter -n -t "${TEST_PLAN}" \
    -Jtarget.host=${HOST} -Jtarget.port=${PORT} \
    -Jvoucher.id=${VOUCHER_ID} \
    -Jthread.count=500 \
    -l "${BASE_REPORT}/result.jtl" \
    -e -o "${BASE_REPORT}/html"

echo "[停止] 停止应用..."
kill ${NOSHARD_PID} 2>/dev/null || true
sleep 5
curl -s -o /dev/null http://localhost:${PORT}/actuator/shutdown 2>/dev/null || true
sleep 3

echo ""
echo "✅ 非分片测试完成"
echo "   报告：${BASE_REPORT}/html/index.html"

# =============================================
# 测试 2：分片模式
# =============================================
echo ""
echo "============================================="
echo "  测试 2/2：分片模式（2 库分片）"
echo "============================================="
echo "  步骤："
echo "    1. 启动应用（profile=sharding）"
echo "    2. 等待启动完成（30s）"
echo "    3. 运行 JMeter 测试"
echo "    4. 停止应用"
echo ""
read -p "  准备好后按 Enter 开始..."

SHARD_REPORT="${REPORT_DIR}/sharded"
mkdir -p "${SHARD_REPORT}"

echo "[启动] 分片模式..."
nohup java -jar target/dianping-1.0.0.jar \
    --server.port=${PORT} \
    --spring.profiles.active=sharding \
    > /tmp/sharding-test-sharding.log 2>&1 &
SHARD_PID=$!

echo "[等待] 应用启动（40s）..."
sleep 40

echo "[测试] 运行 JMeter..."
jmeter -n -t "${TEST_PLAN}" \
    -Jtarget.host=${HOST} -Jtarget.port=${PORT} \
    -Jvoucher.id=${VOUCHER_ID} \
    -Jthread.count=500 \
    -l "${SHARD_REPORT}/result.jtl" \
    -e -o "${SHARD_REPORT}/html"

echo "[停止] 停止应用..."
kill ${SHARD_PID} 2>/dev/null || true
sleep 5

# =============================================
# 结果对比
# =============================================
echo ""
echo "============================================="
echo "  对比结果"
echo "============================================="
echo ""
echo "  基线（非分片）：${BASE_REPORT}/html/index.html"
echo "  分片模式：     ${SHARD_REPORT}/html/index.html"
echo ""
echo "  关注指标："
echo "    ├─ TPS（吞吐量）：分片后是否提升 50%+"
echo "    ├─ P50 延迟：   是否降低"
echo "    ├─ P95 延迟：   高并发下是否明显改善"
echo "    └─ P99 延迟：   尾部延迟是否稳定"
echo ""
echo "  ★ 核心观察点：高并发（1000+）下，分片的 tb_voucher_order"
echo "    将写入分散到 2 个数据库，锁竞争降低，TPS 应该显著提升"
echo ""
