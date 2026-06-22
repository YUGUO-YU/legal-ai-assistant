-- ============================================================
-- 密码哈希迁移脚本：BCrypt -> SHA-256
-- 原因：项目弃用 spring-security-crypto 依赖，
--      统一使用 commons-codec 的 SHA-256Hex(password) 校验。
-- 执行：mysql -u root -p legal_ai < migration-sha256-password.sql
-- ============================================================

-- 后台管理员：admin / admin123
UPDATE admin_user
SET password = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE username = 'admin';

-- 前台用户：demo / demo123
UPDATE frontend_user
SET password = 'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791'
WHERE username = 'demo';

-- 验证
SELECT username, password, real_name FROM admin_user;
SELECT username, password, real_name FROM frontend_user;