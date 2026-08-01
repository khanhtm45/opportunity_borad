# BACKEND — HƯỚNG DẪN CHẠY & LIÊN KẾT DATABASE

## Database: Supabase PostgreSQL 17.6 (đã liên kết)
- Host: `aws-0-ap-northeast-1.pooler.supabase.com:6543`
- DB: `postgres`, User: `postgres.cmmypmpxisysrnqoqfdv`
- Schema đã tạo: 17 objects (16 bảng + 1 VIEW), 7 category đã seed.
- Quản lý schema thủ công qua `docs/db/schema.sql` (Flyway bị tắt vì Flyway 10.10 chưa hỗ trợ PG17).

## JDBC URL (đã tối ưu cho Supabase pooler)
```
jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0&preparedStatementCacheSize=0&defaultRowFetchSize=50&stringtype=unspecified
```
- `prepareThreshold=0` + `preparedStatementCacheSize=0`: tắt server-side prepared statements (tránh lỗi "prepared statement S_1 already exists" của PgBouncer transaction pooling).
- `stringtype=unspecified`: gửi enum dưới dạng unknown để PG tự cast (tránh lỗi enum type mismatch).

## Chạy local
```bash
# build
mvn clean package -DskipTests
# chạy (password từ env hoặc default trong yml)
export DB_PASSWORD='KhanhMinh0@'
java -jar target/opportunity-board-backend-0.1.0.jar
# App lắng nghe http://localhost:8080
```\

## Smoke test (đã verify trên Supabase thật)
```bash
# 1. Board công khai
curl http://localhost:8080/api/v1/opportunities
# 2. Đăng ký student -> trả JWT
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"role":"STUDENT","email":"sv.test1@example.com","password":"password123","fullName":"Test"}'
# 3. Verify email (set ACTIVE qua SQL hoặc endpoint /auth/verify-email)
# 4. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sv.test1@example.com","password":"password123"}'
```

## Các lỗi đã giải quyết khi liên kết Supabase
1. Circular dependency `UserRepositoryHolder` → bỏ, dùng `SecurityUtil` component có inject `UserRepository`.
2. Repository/Entity không được scan → main class đặt tại package gốc `com.opportunityboard`.
3. Flyway `Unsupported Database: PostgreSQL 17.6` → tắt Flyway (`enabled: false`), quản lý schema thủ công.
4. `prepared statement S_1 already exists` (PgBouncer) → `prepareThreshold=0`.
5. Enum type mismatch khi insert → `stringtype=unspecified`.
6. `notification_preferences` thiếu cột `id` → đổi sang PK composite (user_id, type, channel) khớp schema.
7. `findByUserId` trên preference → đổi `findByUserUserId`.

## Bảo mật lưu ý
- Password DB đang nằm trong `application.yml` dưới dạng `${DB_PASSWORD:default}`. Khi deploy thực tế, truyền qua env/secret, KHÔNG commit password thật.
- JWT secret mặc định yếu → set `JWT_SECRET` mạnh khi deploy.
