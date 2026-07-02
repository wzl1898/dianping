#!/bin/bash
# 批量生成用户 token，供 JMeter CSV Data Set Config 使用
# 用法: bash gen-test-users.sh <host> <port> <count> <output_csv>
HOST=${1:-localhost}
PORT=${2:-8080}
COUNT=${3:-500}
CSV=${4:-/tmp/seckill-users-jmeter.csv}

BASE="http://${HOST}:${PORT}"
echo "phone,token" > "$CSV"

gen_user() {
  local i=$1
  local PHONE=$(printf "139%08d" $i)
  curl -s -X POST "${BASE}/api/user/code" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"$PHONE\"}" > /dev/null 2>&1
  local CODE=$(redis-cli -p 6380 GET "login:code:${PHONE}" 2>/dev/null)
  [ -z "$CODE" ] && return 1
  local TOKEN=$(curl -s -X POST "${BASE}/api/user/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"$PHONE\",\"code\":\"$CODE\"}" | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('data',''))" 2>/dev/null)
  [ -n "$TOKEN" ] && echo "$PHONE,$TOKEN"
}
export -f gen_user
export BASE

seq 1 $COUNT | xargs -P 50 -I {} bash -c 'gen_user "$@"' _ {} >> "$CSV" 2>/dev/null

echo "DONE: $(tail -n +2 $CSV | wc -l) users generated"
