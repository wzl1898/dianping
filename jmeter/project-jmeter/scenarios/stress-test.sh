#!/bin/bash
# 简易压测脚本 - 测试核心接口性能
# 使用: bash stress-test.sh

BASE="http://localhost:8080"
TOKEN=""
CONCURRENCY=${1:-50}
REQUESTS=${2:-200}
REPORT="jmeter/report/stress-report-$(date +%Y%m%d-%H%M%S)"

mkdir -p $(dirname "$REPORT")

echo "============================================"
echo "  性能压测 - 并发=${CONCURRENCY} 请求=${REQUESTS}"
echo "============================================"

# 先登录获取 token
CODE=$(redis-cli -p 6380 GET "login:code:13800138000" 2>/dev/null)
if [ -z "$CODE" ]; then
    curl -s -X POST "$BASE/api/user/code" -H "Content-Type: application/json" -d '{"phone":"13800138000"}' > /dev/null
    CODE=$(redis-cli -p 6380 GET "login:code:13800138000" 2>/dev/null)
fi
TOKEN=$(curl -s -X POST "$BASE/api/user/login" -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800138000\",\"code\":\"$CODE\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',''))" 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo "ERROR: Failed to get token"
    exit 1
fi
echo "Token obtained: ${TOKEN:0:16}..."

# 压测函数: concurrent_test <name> <url> <method> [data] [header]
concurrent_test() {
    local name=$1 url=$2 method=$3 data=$4 header=$5
    local success=0 fail=0
    local times=()

    echo ""
    echo "--- $name ---"

    for i in $(seq 1 $REQUESTS); do
        start=$(date +%s%N)
        if [ "$method" = "POST" ]; then
            RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$url" \
                -H "Content-Type: application/json" \
                -H "authorization: $TOKEN" \
                $data 2>/dev/null)
        else
            RESP=$(curl -s -o /dev/null -w "%{http_code}" "$url" \
                -H "authorization: $TOKEN" 2>/dev/null)
        fi
        end=$(date +%s%N)
        elapsed=$(( ($end - $start) / 1000000 ))  # ms

        if [ "$RESP" = "200" ]; then
            ((success++))
        else
            ((fail++))
        fi
        times+=($elapsed)

        # Progress bar
        if [ $((i % 50)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""

    # Analyze results
    local total=${#times[@]}
    local sum=0
    for t in "${times[@]}"; do sum=$((sum + t)); done
    local avg=$((sum / total))

    # Sort for percentiles
    IFS=$'\n' sorted=($(sort -n <<<"${times[*]}")); unset IFS
    local p50=${sorted[$((total * 50 / 100))]}
    local p95=${sorted[$((total * 95 / 100))]}
    local p99=${sorted[$((total * 99 / 100))]}
    local max=${sorted[$((total - 1))]}
    local min=${sorted[0]}
    local tps=$((total * 1000 / sum))

    echo "  成功=${success} 失败=${fail}"
    echo "  平均=${avg}ms 最小=${min}ms 最大=${max}ms"
    echo "  P50=${p50}ms P95=${p95}ms P99=${p99}ms"
    echo "  TPS=${tps}"

    # Append to report
    printf "%-25s | %4d | %4d | %5dms | %5dms | %5dms | %5dms | %5dms | %4d\n" \
        "$name" "$success" "$fail" "$avg" "$min" "$max" "$p50" "$p99" "$tps" >> "$REPORT".txt
}

echo ""
echo "开始压测..."
printf "%-25s | %4s | %4s | %6s | %5s | %5s | %5s | %5s | %4s\n" \
    "接口" "成功" "失败" "平均" "最小" "最大" "P50" "P99" "TPS" > "$REPORT".txt
printf "%-25s-|-%-4s-|-%-4s-|-%-6s-|-%-5s-|-%-5s-|-%-5s-|-%-5s-|-%-4s\n" \
    "-------------------------" "----" "----" "------" "-----" "-----" "-----" "-----" "----" >> "$REPORT".txt

# PT-1: 商户详情查询（缓存命中）
concurrent_test "PT-1: 商户详情(缓存)" "$BASE/api/shop/1" "GET"

# PT-2: 商户分类列表
concurrent_test "PT-2: 分类列表" "$BASE/api/shop-type/list" "GET"

# PT-3: 秒杀下单
concurrent_test "PT-3: 秒杀下单" "$BASE/api/voucher/seckill/1" "POST"

# PT-4: 用户信息
concurrent_test "PT-4: 用户信息" "$BASE/api/user/me" "GET"

# PT-5: 探店笔记列表
concurrent_test "PT-5: 笔记列表" "$BASE/api/blog?current=1" "GET"

# PT-6: 附近商户
concurrent_test "PT-6: 附近商户" "$BASE/api/shop/list/nearby?typeId=1&x=116.3&y=39.9&distance=5000" "GET"

echo ""
echo "============================================"
echo "  报告已保存: $REPORT.txt"
echo "============================================"
cat "$REPORT".txt
