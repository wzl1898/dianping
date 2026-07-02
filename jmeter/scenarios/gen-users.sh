#!/bin/bash
# 生成 500 个测试用户 + 登录获取 token
# 输出: /tmp/seckill-users.csv (phone,token)

BASE="http://localhost:8080"
CSV="/tmp/seckill-users.csv"
> "$CSV"
echo "phone,token" >> "$CSV"

TOTAL=500
SUCCESS=0
FAIL=0

for i in $(seq 1 $TOTAL); do
  PHONE=$(printf "139%08d" $i)  # 13900000001 ~ 13900000500

  # 发验证码
  curl -s -X POST "$BASE/api/user/code" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"$PHONE\"}" > /dev/null 2>&1

  # 从 Redis 拿验证码 (6380)
  CODE=$(redis-cli -p 6380 GET "login:code:${PHONE}" 2>/dev/null)
  if [ -z "$CODE" ]; then
    # 重试一次
    curl -s -X POST "$BASE/api/user/code" \
      -H "Content-Type: application/json" \
      -d "{\"phone\":\"$PHONE\"}" > /dev/null 2>&1
    sleep 0.2
    CODE=$(redis-cli -p 6380 GET "login:code:${PHONE}" 2>/dev/null)
  fi

  if [ -z "$CODE" ]; then
    FAIL=$((FAIL + 1))
    continue
  fi

  # 登录
  TOKEN=$(curl -s -X POST "$BASE/api/user/login" \
    -H "Content-Type: application/json" \
    -d "{\"phone\":\"$PHONE\",\"code\":\"$CODE\"}" | \
    python3 -c "import sys,json; print(json.load(sys.stdin).get('data',''))" 2>/dev/null)

  if [ -n "$TOKEN" ] && [ ${#TOKEN} -gt 10 ]; then
    echo "${PHONE},${TOKEN}" >> "$CSV"
    SUCCESS=$((SUCCESS + 1))
  else
    FAIL=$((FAIL + 1))
  fi

  if [ $((i % 50)) -eq 0 ]; then
    echo "Progress: $i/$TOTAL (ok=$SUCCESS fail=$FAIL)"
  fi
done

echo ""
echo "========================================"
echo "  Done: ok=$SUCCESS fail=$FAIL"
echo "  File: $CSV"
echo "========================================"
