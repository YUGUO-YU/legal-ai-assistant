-- 在你的本地 MySQL 环境执行此脚本修复密码哈希
-- 账号 root 密码 main0126（与 application.yml 中一致）

UPDATE admin_user
SET password = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE username = 'admin';

UPDATE frontend_user
SET password = 'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791'
WHERE username = 'demo';

-- 验证两表
SELECT 'admin_user' AS tbl, username, LEFT(password, 16) AS hash_prefix, status FROM admin_user WHERE username='admin'
UNION ALL
SELECT 'frontend_user' AS tbl, username, LEFT(password, 16) AS hash_prefix, status FROM frontend_user WHERE username='demo';