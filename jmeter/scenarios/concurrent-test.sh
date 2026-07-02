#!/bin/bash
# 并发压测脚本 - 使用 xargs -P 实现并行请求
# 用法: bash concurrent-test.sh [并发数]

BASE="http://localhost:8080"
CONCURRENCY=${1:-30}
REPORT_DIR="jmeter/report/benchmark-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$REPORT_DIR"

# 登录
CODE=$(redis-cli -p 6380 GET "login:code:13800138000" 2>/dev/null)
if [ -z "$CODE" ]; then
  curl -s -X POST "$BASE/api/user/code" -H "Content-Type: application/json" \
    -d '{"phone":"13800138000"}' > /dev/null
  CODE=$(redis-cli -p 6380 GET "login:code:13800138000" 2>/dev/null)
fi
TOKEN=$(curl -s -X POST "$BASE/api/user/login" -H "Content-Type: application/json" \
  -d "{\"phone\":\"13800138000\",\"code\":\"$CODE\"}" | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('data',''))" 2>/dev/null)
echo "Token: ${TOKEN:0:16}..."
echo "Benchmark: ${CONCURRENCY} concurrent, 100 requests each"
echo ""

bench() {
  local name=$1 url=$2 method=$3
  local result_file="$REPORT_DIR/$(echo $name | tr ' ' '_').txt"
  > "$result_file"

  echo -n "  $name ... "

  run_one() {
    local url=$1 method=$2 token=$3 result_file=$4
    local start=$(date +%s%N)
    if [ "$method" = "POST" ]; then
      curl -s -o /dev/null -w "%{http_code}" -X POST "$url" \
        -H "Content-Type: application/json" -H "authorization: $token" \
        -d '{}' 2>/dev/null
    else
      curl -s -o /dev/null -w "%{http_code}" "$url" \
        -H "authorization: $token" 2>/dev/null
    fi
    local end=$(date +%s%N)
    local elapsed=$(( (end - start) / 1000000 ))
    echo "$elapsed" >> "$result_file"
  }
  export -f run_one

  seq 1 100 | xargs -P $CONCURRENCY -I {} bash -c \
    "run_one '$url' '$method' '$TOKEN' '$result_file'" 2>/dev/null

  local latencies=()
  while IFS= read -r line; do
    [[ "$line" =~ ^[0-9]+$ ]] && latencies+=($line)
  done < "$result_file"

  local count=${#latencies[@]}
  [ $count -eq 0 ] && { echo "0 results"; return; }

  local sum=0; for t in "${latencies[@]}"; do sum=$((sum + t)); done
  local avg=$((sum / count))

  IFS=$'\n' sorted=($(sort -n <<<"${latencies[*]}")); unset IFS
  local p50=${sorted[$((count * 50 / 100))]}
  local p95=${sorted[$((count * 95 / 100))]}
  local p99=${sorted[$((count * 99 / 100))]}
  local min=${sorted[0]}; local max=${sorted[$((count - 1))]}

  # TPS = total requests / total seconds
  local total_sec=$((sum / 1000))
  [ $total_sec -eq 0 ] && local tps=$count || local tps=$((count / total_sec))

  echo "avg=${avg}ms p50=${p50}ms p95=${p95}ms tps=${tps}"
  echo "$name,$count,$avg,$min,$max,$p50,$p95,$p99,$tps" >> "$REPORT_DIR/summary.csv"
}

echo "接口,请求数,平均,最小,最大,P50,P95,P99,TPS" > "$REPORT_DIR/summary.csv"
bench "商户分类列表" "$BASE/api/shop-type/list" "GET"
bench "商户详情(缓存)" "$BASE/api/shop/1" "GET"
bench "附近商户(GEO)" "$BASE/api/shop/list/nearby?typeId=1\&x=116.3\&y=39.9\&distance=5000" "GET"
bench "用户信息" "$BASE/api/user/me" "GET"
bench "秒杀下单" "$BASE/api/voucher/seckill/1" "POST"

echo ""
echo "=============================================="
echo "  SUMMARY"
echo "=============================================="
column -t -s',' "$REPORT_DIR/summary.csv" 2>/dev/null || cat "$REPORT_DIR/summary.csv"
echo ""
echo "Report: $REPORT_DIR/summary.csv"
