-- 法律AI助手数据库初始化脚本
-- 运行前请确保 MySQL 已启动，并创建空数据库

-- 创建数据库
CREATE DATABASE IF NOT EXISTS legal_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE legal_ai;

-- ==================== 基础设施表 ====================

CREATE TABLE IF NOT EXISTS admin_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password        VARCHAR(128) COMMENT 'SHA-256 加密',
    real_name       VARCHAR(64) NOT NULL,
    mobile          VARCHAR(20),
    email           VARCHAR(128),
    avatar          VARCHAR(500),
    feishu_union_id VARCHAR(128),
    user_type       TINYINT DEFAULT 1 COMMENT '1后台 2业务只读',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用 2锁定',
    last_login_at   DATETIME,
    last_login_ip   VARCHAR(64),
    team_id         BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_team (team_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '后台用户';

CREATE TABLE IF NOT EXISTS frontend_user (
    id              VARCHAR(64) PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password        VARCHAR(128) COMMENT 'SHA-256 加密',
    real_name       VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    avatar          VARCHAR(500),
    bio             TEXT,
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    approved        TINYINT DEFAULT 0 COMMENT '0待审核 1已审核',
    last_login_at   DATETIME,
    last_login_ip   VARCHAR(64),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status),
    INDEX idx_approved (approved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '前端用户';

CREATE TABLE IF NOT EXISTS auth_tokens (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    token           VARCHAR(128) NOT NULL UNIQUE,
    user_id         VARCHAR(64) NOT NULL,
    username        VARCHAR(64),
    expire_at       DATETIME NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证令牌表';

CREATE TABLE IF NOT EXISTS admin_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(64) NOT NULL UNIQUE,
    role_name   VARCHAR(64) NOT NULL,
    data_scope  TINYINT DEFAULT 4 COMMENT '1本人 2本部门 3本团队 4全部',
    status      TINYINT DEFAULT 1,
    remark      VARCHAR(500),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '后台角色';

CREATE TABLE IF NOT EXISTS admin_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT DEFAULT 0,
    menu_name   VARCHAR(64) NOT NULL,
    menu_type   TINYINT COMMENT '1目录 2菜单 3按钮',
    path        VARCHAR(200),
    component   VARCHAR(200),
    permission  VARCHAR(128),
    icon        VARCHAR(64),
    sort_order  INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    biz_module  VARCHAR(32) COMMENT 'MOD-01..MOD-10',
    INDEX idx_module (biz_module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '后台菜单';

CREATE TABLE IF NOT EXISTS admin_role_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    UNIQUE KEY uk_rm (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    UNIQUE KEY uk_ur (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT,
    username        VARCHAR(64),
    operation       VARCHAR(64) NOT NULL,
    biz_module      VARCHAR(32),
    biz_type        VARCHAR(64),
    biz_id          VARCHAR(64),
    request_url     VARCHAR(500),
    request_method  VARCHAR(10),
    request_params  TEXT,
    response_result TEXT,
    ip              VARCHAR(64),
    duration_ms     INT,
    status          TINYINT COMMENT '1成功 0失败',
    error_msg       TEXT,
    trace_id        VARCHAR(64),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_module_biz (biz_module, biz_type),
    INDEX idx_created (created_at),
    INDEX idx_audit_user_module (user_id, biz_module),
    INDEX idx_audit_operation (operation),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '审计日志';

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

-- 合同审查记录表
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

-- ==================== 默认数据 ====================

-- 后台用户 (用户名: admin, 密码: admin123)
INSERT INTO admin_user (id, username, password, real_name, status) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '超级管理员', 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), real_name = VALUES(real_name), status = VALUES(status);

-- 前端用户 (用户名: demo, 密码: demo123)
INSERT INTO frontend_user (id, username, password, real_name, email, status, approved) VALUES
('u-001', 'demo', 'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', '演示用户', 'demo@legal-ai.local', 1, 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), real_name = VALUES(real_name), email = VALUES(email), status = VALUES(status), approved = VALUES(approved);

-- 角色
INSERT IGNORE INTO admin_role (id, role_code, role_name, data_scope, status, remark) VALUES
(1, 'SUPER_ADMIN', '超级管理员', 4, 1, '全部权限'),
(2, 'OPS_ADMIN', '运营管理员', 4, 1, '基础+数据资产+运营+系统配置'),
(3, 'LEGAL_ADMIN', '法务管理员', 4, 1, '数据资产'),
(4, 'DEV_OPS', '运维工程师', 4, 1, '基础+监控+系统配置+AI域'),
(5, 'AUDITOR', '审计员', 4, 1, '只读 审计+监控+运营'),
(6, 'READONLY', '只读访客', 4, 1, '全部只读');

-- 角色-用户授权
INSERT IGNORE INTO admin_user_role (user_id, role_id) VALUES (1, 1);

-- ==================== 验证查询 ====================
SELECT '=== admin_user 表数据 ===' as '';
SELECT * FROM admin_user;

SELECT '=== frontend_user 表数据 ===' as '';
SELECT * FROM frontend_user;

SELECT '=== admin_role 表数据 ===' as '';
SELECT * FROM admin_role;
