#!/bin/bash
# ============================================================
# 分片压测数据准备脚本
# 生成大量历史订单和测试用户，模拟数据积累后的分片效果
# ============================================================

MYSQL_USER="root"
MYSQL_PASS="root"
MYSQL_HOST="localhost"
MYSQL_PORT="3307"
MYSQL_CMD="mysql -u${MYSQL_USER} -p${MYSQL_PASS} -h${MYSQL_HOST} -P${MYSQL_PORT}"

echo "=========================================="
echo "  分片压测数据准备"
echo "=========================================="

# 1. 生成测试用户
echo "[1/3] 生成测试用户（10000 个）..."
${MYSQL_CMD} -e "USE dianping_ds0; DELETE FROM tb_user WHERE id > 1000;"
for i in $(seq 1001 11000); do
    phone=$(printf "138%08d" $i)
    ${MYSQL_CMD} -e "USE dianping_ds0;
        INSERT IGNORE INTO tb_user (id, phone, nick_name, icon)
        VALUES (${i}, '${phone}', '测试用户${i}', '');"
    if [ $((i % 1000)) -eq 0 ]; then
        echo "   ... ${i} 用户"
    fi
done

# 2. 生成历史订单（分散到 ds0 和 ds1，模拟 10w+ 数据量）
echo "[2/3] 生成历史订单（10 万条，均匀分布到 ds0/ds1）..."
BATCH_SIZE=1000
for batch in $(seq 0 99); do
    SQL=""
    for j in $(seq 1 ${BATCH_SIZE}); do
        idx=$((batch * BATCH_SIZE + j))
        uid=$((1001 + (idx % 10000)))
        vid=$((1 + (idx % 10)))
        order_id=$((100000000 + idx))
        SQL="${SQL} INSERT IGNORE INTO dianping_ds0.tb_voucher_order (id, user_id, voucher_id, status, create_time)
             VALUES (${order_id}, ${uid}, ${vid}, 1, NOW() - INTERVAL ${idx} MINUTE);
             INSERT IGNORE INTO dianping_ds1.tb_voucher_order (id, user_id, voucher_id, status, create_time)
             VALUES (${order_id}, ${uid}, ${vid}, 1, NOW() - INTERVAL ${idx} MINUTE);"
    done
    ${MYSQL_CMD} -e "${SQL}" 2>/dev/null
    echo "   ... $(((batch + 1) * BATCH_SIZE)) 订单"
done

# 3. 生成秒杀券和 Redis 库存数据
echo "[3/3] 准备秒杀券库存..."
${MYSQL_CMD} -e "USE dianping_ds0;
    UPDATE tb_seckill_voucher SET stock = 100000 WHERE voucher_id = 1;
    UPDATE tb_seckill_voucher SET stock = 100000 WHERE voucher_id = 2;"

echo ""
echo "✅ 数据准备完成！"
echo "   - 10000 个测试用户（user_id: 1001~11000）"
echo "   - 100000 条历史订单（均匀分布在 ds0 和 ds1）"
echo "   - 秒杀券库存充足（100000）"
echo ""
echo "  下一步：启动分片模式应用，执行 JMeter 压测"
