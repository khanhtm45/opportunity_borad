#!/bin/bash
PSQL="psql postgresql://postgres.cmmypmpxisysrnqoqfdv:KhanhMinh0%40@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require"
echo "$1" | $PSQL 2>&1
