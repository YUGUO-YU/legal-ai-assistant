-- ============================================================
-- 法律AI助手 数据库迁移脚本
-- 执行方式: mysql -u root -p legal_ai < migration-v2.sql
-- 执行时间: 2026-07-03
-- ============================================================

-- --------------------------------------------------
-- 1. frontend_user 表新增 approved 字段（注册审核）
-- --------------------------------------------------
ALTER TABLE frontend_user ADD COLUMN IF NOT EXISTS approved TINYINT DEFAULT 0 COMMENT '0待审核 1已审核';
ALTER TABLE frontend_user ADD INDEX idx_approved (approved);

-- 将已有用户标记为已审核（兼容存量数据）
UPDATE frontend_user SET approved = 1 WHERE approved = 0 AND id IN (SELECT * FROM (SELECT id FROM frontend_user WHERE status = 1 LIMIT 1) AS t);

-- --------------------------------------------------
-- 2. 系统公告表
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_announcement (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(256) NOT NULL,
    content         TEXT NOT NULL,
    type            TINYINT DEFAULT 1 COMMENT '1=系统公告 2=功能更新 3=维护通知 4=安全警告',
    priority        TINYINT DEFAULT 0 COMMENT '0=普通 1=重要 2=紧急',
    status          TINYINT DEFAULT 1 COMMENT '1=发布 0=草稿',
    published_at    DATETIME,
    expired_at      DATETIME,
    created_by      VARCHAR(64),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_published (status, published_at),
    INDEX idx_type (type),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统公告';

-- --------------------------------------------------
-- 3. 默认公告数据
-- --------------------------------------------------
INSERT IGNORE INTO sys_announcement (id, title, content, type, priority, status, published_at, created_by) VALUES
(1, '法律AI助手系统正式上线', '欢迎使用法律AI助手系统，本系统提供智能法律咨询、文书生成、案件分析等功能。如有疑问请联系管理员。', 1, 0, 1, NOW(), 'admin'),
(2, '系统功能更新通知 v2.1', '本次更新：1) 新增前端用户注册审核功能；2) 优化审计日志查看体验；3) 修复若干已知问题。', 2, 0, 1, NOW(), 'admin'),
(3, '数据备份通知', '系统将于每周日凌晨2:00-6:00进行数据备份，届时系统服务可能短暂中断，请提前做好准备。', 3, 1, 1, NOW(), 'admin'),
(4, '账户安全提醒', '请勿将账户密码告知他人，定期更换密码。如发现异常登录请立即联系管理员。', 4, 1, 1, NOW(), 'admin');

-- --------------------------------------------------
-- 4. 审计日志表新增响应结果字段（如果不存在）
-- --------------------------------------------------
-- 审计日志表的 request_params 和 response_result 已经是 TEXT 类型，无需修改

-- --------------------------------------------------
-- 5. 密码重置验证码表（持久化存储，替代内存Map）
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_code (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(64) NOT NULL,
    code            VARCHAR(8) NOT NULL,
    expire_at       DATETIME NOT NULL,
    used            TINYINT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '密码重置验证码';

-- --------------------------------------------------
-- 6. 用户登录历史表
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS user_login_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(64) NOT NULL,
    username        VARCHAR(64),
    ip              VARCHAR(64),
    user_agent      VARCHAR(500),
    login_type      VARCHAR(16) COMMENT 'frontend/admin',
    login_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_login_at (login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户登录历史';

-- --------------------------------------------------
-- 7. 合同审查记录表（持久化存储，替代内存Map）
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS contract_review (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_uuid     VARCHAR(64) NOT NULL UNIQUE COMMENT '审查记录UUID',
    user_id         BIGINT COMMENT '用户ID',
    username        VARCHAR(64),
    file_name       VARCHAR(255),
    file_size       BIGINT,
    review_type     VARCHAR(32) COMMENT 'risk_detection/full_review',
    risk_level      VARCHAR(20) COMMENT 'low/medium/high/critical',
    risk_count      INT DEFAULT 0,
    summary         TEXT,
    risk_details    LONGTEXT COMMENT 'JSON格式的风险点列表',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '合同审查记录';

-- --------------------------------------------------
-- 8. 审计日志表添加复合索引
-- --------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_audit_user_module ON admin_audit_log(user_id, biz_module);
CREATE INDEX IF NOT EXISTS idx_audit_operation ON admin_audit_log(operation);
CREATE INDEX IF NOT EXISTS idx_audit_created ON admin_audit_log(created_at);

-- --------------------------------------------------
-- 完成提示
-- --------------------------------------------------
SELECT 'Migration v2 completed successfully' AS status;
SELECT COUNT(*) AS announcements_created FROM sys_announcement;
SELECT COUNT(*) AS users_approved FROM frontend_user WHERE approved = 1;
