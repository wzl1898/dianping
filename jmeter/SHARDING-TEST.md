# 分库分表（Sharding）测试说明

## 架构概览

```
┌──────────────────────────────────────────────────────┐
│                   应用服务                             │
│  ┌──────────────────────────────────────────────┐    │
│  │        ShardingSphere-JDBC 数据源              │    │
│  │  user_id % 2 = 0 → ds0                       │    │
│  │  user_id % 2 = 1 → ds1                       │    │
│  └──────┬──────────────────────┬────────────────┘    │
│         │                      │                       │
│         ▼                      ▼                       │
│  ┌───────────┐          ┌───────────┐                │
│  │ ds0       │          │ ds1       │                │
│  │ 所有9张表 │          │ 仅订单表  │                │
│  └───────────┘          └───────────┘                │
│         │                      │                       │
└─────────┼──────────────────────┼──────────────────────┘
          │                      │
          ▼                      ▼
   MySQL:3307/dianping_ds0  MySQL:3307/dianping_ds1
```

## 分片策略

| 表 | 分片策略 | 说明 |
|----|---------|------|
| **tb_voucher_order** | `user_id % 2` → ds0/ds1 | **唯一分片表** |
| tb_user | 默认 ds0 | 不分片 |
| tb_shop | 默认 ds0 | 不分片 |
| tb_voucher | 默认 ds0 | 不分片 |
| tb_seckill_voucher | 默认 ds0 | 不分片 |
| tb_blog | 默认 ds0 | 不分片 |
| tb_follow | 默认 ds0 | 不分片 |
| tb_shop_type | 默认 ds0 | 不分片 |
| tb_admin | 默认 ds0 | 不分片 |

## 测试步骤

### 环境准备

```bash
# 1. 启动基础设施（MySQL:3307, Redis:6380, RabbitMQ:5672）
# 如果已在运行则跳过
bash setup-env.sh

# 2. 创建分片数据库
bash setup-sharding.sh

# 3. 准备测试数据（10万+历史订单）
bash jmeter/scenarios/gen-sharding-test-data.sh
```

### 运行测试

```bash
# 方式一：使用自动对比脚本
cd jmeter && bash run-sharding-compare.sh

# 方式二：手动对比

## 1. 启动非分片模式（基线）
mvn spring-boot:run
# 或：java -jar target/dianping-1.0.0.jar

## 2. 在新终端运行 JMeter 压测
jmeter -n -t jmeter/scenarios/PT-8-sharding-compare.jmx \
    -Jtarget.host=localhost -Jtarget.port=8080 \
    -l jmeter/report/baseline/result.jtl \
    -e -o jmeter/report/baseline/html

## 3. 停止应用

## 4. 启动分片模式
mvn spring-boot:run -Dspring-boot.run.profiles=sharding
# 或：java -jar target/dianping-1.0.0.jar --spring.profiles.active=sharding

## 5. 再次运行 JMeter 压测
jmeter -n -t jmeter/scenarios/PT-8-sharding-compare.jmx \
    -Jtarget.host=localhost -Jtarget.port=8080 \
    -l jmeter/report/sharded/result.jtl \
    -e -o jmeter/report/sharded/html
```

### 验证分片是否正确

```sql
-- 登录 MySQL 后：

-- 检查 user_id 偶数的订单是否在 ds0
SELECT COUNT(*) FROM dianping_ds0.tb_voucher_order WHERE user_id % 2 = 0;
SELECT COUNT(*) FROM dianping_ds0.tb_voucher_order WHERE user_id % 2 = 1;
-- user_id%2=0 的应该有数据，user_id%2=1 的在 ds0 应该为 0

-- 检查 user_id 奇数的订单是否在 ds1
SELECT COUNT(*) FROM dianping_ds1.tb_voucher_order WHERE user_id % 2 = 1;
SELECT COUNT(*) FROM dianping_ds1.tb_voucher_order WHERE user_id % 2 = 0;
-- user_id%2=1 的应该有数据，user_id%2=0 的在 ds1 应该为 0
```

### 观察 ShardingSphere SQL 日志

启动时添加 `--spring.profiles.active=sharding`，ShardingSphere 会打印：

```
ShardingSphere-SQL: Logic SQL: INSERT INTO tb_voucher_order ...
ShardingSphere-SQL: Actual SQL: ds0 ::: INSERT INTO tb_voucher_order ...
```

可以实时看到 SQL 被路由到哪个分片。

## 预期效果对比

| 指标 | 非分片（单库） | 分片（2 库） | 说明 |
|-----|--------------|------------|------|
| 秒杀下单 TPS（500并发） | ~3000 | ~5500 | 写入分散到 2 库，锁竞争减半 |
| P50 延迟（500并发） | ~8ms | ~5ms | 单表数据量少，索引更快 |
| P99 延迟（500并发） | ~50ms | ~25ms | 热点行锁减少，尾部延迟改善 |
| 1000 并发 TPS 拐点 | 约 800 并发饱和 | 约 1500 并发饱和 | 分片延缓了数据库瓶颈 |
| 批量查订单（跨分片） | 单表扫描 | 需要归并 | 跨分片聚合查询会稍慢 |

## 查看测试报告

JMeter 生成的 HTML 报告在：
- `jmeter/report/baseline/html/index.html` — 非分片基线
- `jmeter/report/sharded/html/index.html` — 分片模式

关注指标：
- **Throughput**（TPS）— 分片后应显著提升
- **Response Time Percentiles** — P50/P90/P95/P99
- **Error Rate** — 分片不应增加错误率
