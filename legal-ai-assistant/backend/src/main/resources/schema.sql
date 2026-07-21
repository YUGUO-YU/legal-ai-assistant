-- ============================================================
-- 法律AI助手数据库完整安装脚本
-- 适用于 MySQL 8.0+
-- 使用方式: mysql -u root -p < legal-ai-full.sql
-- 或: source legal-ai-full.sql
-- ============================================================

-- --------------------------------------------------
-- 第一部分：创建数据库
-- --------------------------------------------------
CREATE DATABASE IF NOT EXISTS legal_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE legal_ai;

-- --------------------------------------------------
-- 第二部分：基础设施表（用户、角色、菜单、认证）
-- --------------------------------------------------

-- 后台用户表
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

-- 前端用户表
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

-- 认证令牌表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '认证令牌表';

-- 角色表
CREATE TABLE IF NOT EXISTS admin_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code       VARCHAR(64) NOT NULL UNIQUE,
    role_name       VARCHAR(64) NOT NULL,
    data_scope      TINYINT DEFAULT 4 COMMENT '1本人 2本部门 3本团队 4全部',
    status          TINYINT DEFAULT 1,
    remark          VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '后台角色';

-- 菜单表
CREATE TABLE IF NOT EXISTS admin_menu (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id       BIGINT DEFAULT 0,
    menu_name       VARCHAR(64) NOT NULL,
    menu_type       TINYINT COMMENT '1目录 2菜单 3按钮',
    path            VARCHAR(200),
    component       VARCHAR(200),
    permission      VARCHAR(128),
    icon            VARCHAR(64),
    sort_order      INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    biz_module      VARCHAR(32) COMMENT 'MOD-01..MOD-10',
    INDEX idx_module (biz_module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '后台菜单';

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS admin_role_menu (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id         BIGINT NOT NULL,
    menu_id         BIGINT NOT NULL,
    UNIQUE KEY uk_rm (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '角色菜单关联';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS admin_user_role (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    UNIQUE KEY uk_ur (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户角色关联';

-- 审计日志表
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

-- 系统公告表
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

-- 密码重置验证码表
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

-- 用户登录历史表
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
-- 第三部分：法规与案例表
-- --------------------------------------------------

-- 法规主表
CREATE TABLE IF NOT EXISTS law_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    law_uuid        VARCHAR(64) NOT NULL UNIQUE COMMENT '法规UUID',
    title           VARCHAR(500) NOT NULL COMMENT '法规标题',
    short_title     VARCHAR(200) COMMENT '法规简称',
    category_l1     VARCHAR(50) NOT NULL COMMENT '一级分类',
    category_l2     VARCHAR(100) COMMENT '二级分类',
    issuing_authority VARCHAR(200) COMMENT '发布机关',
    issue_date      DATE COMMENT '发布日期',
    effective_date  DATE COMMENT '生效日期',
    status          TINYINT DEFAULT 1 COMMENT '状态：1现行 2废止 3修订中',
    source_url      VARCHAR(500) COMMENT '来源URL',
    source_name     VARCHAR(100) COMMENT '来源名称',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_l1, category_l2),
    INDEX idx_status (status),
    INDEX idx_issue_date (issue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法规主表';

-- 法规条款表
CREATE TABLE IF NOT EXISTS law_article (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    law_id          BIGINT NOT NULL COMMENT '关联法规ID',
    article_uuid    VARCHAR(64) NOT NULL UNIQUE,
    article_no      VARCHAR(50) NOT NULL COMMENT '条款编号',
    title           VARCHAR(200) COMMENT '条款标题',
    content         TEXT NOT NULL COMMENT '条款正文',
    content_hash    VARCHAR(64) COMMENT '内容哈希',
    sort_order      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (law_id) REFERENCES law_document(id) ON DELETE CASCADE,
    INDEX idx_law_id (law_id),
    INDEX idx_article_no (article_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法规条款表';

-- 法规收藏表
CREATE TABLE IF NOT EXISTS law_favorite (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(64) NOT NULL COMMENT '用户ID',
    law_uuid        VARCHAR(64) NOT NULL COMMENT '法规UUID',
    law_title       VARCHAR(500) COMMENT '法规标题（冗余存储）',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_law (user_id, law_uuid),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法规收藏表';

-- 法规关联表
CREATE TABLE IF NOT EXISTS law_relation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_article_id BIGINT NOT NULL COMMENT '来源条款',
    target_article_id BIGINT NOT NULL COMMENT '目标条款',
    relation_type   VARCHAR(20) COMMENT '引用关系',
    weight          DECIMAL(5,2) DEFAULT 1.0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_relation (source_article_id, target_article_id),
    FOREIGN KEY (source_article_id) REFERENCES law_article(id),
    FOREIGN KEY (target_article_id) REFERENCES law_article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法规关联表';

-- 法规导入历史表
CREATE TABLE IF NOT EXISTS law_import_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_uuid       VARCHAR(64) NOT NULL UNIQUE COMMENT '任务UUID',
    law_name        VARCHAR(500) NOT NULL COMMENT '法律名称',
    source          VARCHAR(20) NOT NULL COMMENT '数据源: web_search/upload/preset',
    status          VARCHAR(20) NOT NULL DEFAULT 'running' COMMENT '状态: running/success/failed',
    total_articles  INT DEFAULT 0 COMMENT '拉取条款数',
    inserted_articles INT DEFAULT 0 COMMENT '新增条款数',
    updated_articles INT DEFAULT 0 COMMENT '更新条款数',
    mysql_ok        TINYINT DEFAULT 0 COMMENT 'MySQL 写入是否成功',
    es_ok           TINYINT DEFAULT 0 COMMENT 'Elasticsearch 写入是否成功',
    milvus_ok       TINYINT DEFAULT 0 COMMENT 'Milvus 写入是否成功',
    error_message   TEXT COMMENT '错误信息',
    operator        VARCHAR(64) DEFAULT 'system' COMMENT '操作者',
    started_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    finished_at     DATETIME,
    INDEX idx_status (status),
    INDEX idx_started (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法规导入历史表';

-- 法规修订历史表
CREATE TABLE IF NOT EXISTS law_revision (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    law_id          BIGINT NOT NULL,
    revision_no     VARCHAR(32),
    revision_type   TINYINT,
    revision_date   DATE,
    revision_note   TEXT,
    source_url      VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_law (law_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法规修订历史';

-- 判例表（外部案例）
CREATE TABLE IF NOT EXISTS legal_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_uuid       VARCHAR(64) NOT NULL UNIQUE,
    case_no         VARCHAR(64) COMMENT '案号',
    title           VARCHAR(500) NOT NULL,
    court           VARCHAR(128) COMMENT '法院名称',
    case_type       VARCHAR(32) COMMENT '案件类型',
    case_cause      VARCHAR(128) COMMENT '案由',
    judgment_date   DATE COMMENT '裁判日期',
    summary         TEXT COMMENT '裁判摘要',
    full_text_url   VARCHAR(500) COMMENT '全文URL',
    source_url      VARCHAR(500) COMMENT '来源URL',
    source_name     VARCHAR(100) COMMENT '来源名称',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type),
    INDEX idx_court (court),
    INDEX idx_judgment_date (judgment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '判例表';

-- 案例主表
CREATE TABLE IF NOT EXISTS tb_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_uuid       VARCHAR(64) NOT NULL UNIQUE,
    case_no         VARCHAR(64) COMMENT '案号',
    case_name       VARCHAR(500) NOT NULL,
    case_type       TINYINT COMMENT '案件类型：1民事 2刑事 3行政',
    case_cause      VARCHAR(128) COMMENT '案由',
    court_level     TINYINT COMMENT '法院层级：1最高院 2高院 3中院 4基层',
    court_name      VARCHAR(128) COMMENT '法院名称',
    judge_date      DATE COMMENT '裁判日期',
    trial_procedure VARCHAR(20) COMMENT '审理程序',
    judgment_result TINYINT COMMENT '裁判结果：1全部支持 2部分支持 3驳回',
    litigation_amount DECIMAL(18,2) COMMENT '诉讼金额',
    plaintiff       VARCHAR(256) COMMENT '原告',
    defendant       VARCHAR(256) COMMENT '被告',
    key_facts       TEXT COMMENT '关键事实',
    judgment_summary TEXT COMMENT '裁判摘要',
    legal_basis     JSON COMMENT '法律依据',
    vector_status   TINYINT DEFAULT 0 COMMENT '向量化状态',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type),
    INDEX idx_case_cause (case_cause),
    INDEX idx_court_level (court_level),
    INDEX idx_judge_date (judge_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '案例主表';

-- 案例要素表
CREATE TABLE IF NOT EXISTS tb_case_element (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id         BIGINT NOT NULL,
    element_type    VARCHAR(32) NOT NULL COMMENT '要素类型',
    element_key     VARCHAR(128) COMMENT '要素key',
    element_value   TEXT NOT NULL COMMENT '要素值',
    importance      DECIMAL(3,2) DEFAULT 1.0 COMMENT '重要程度权重',
    FOREIGN KEY (case_id) REFERENCES tb_case(id) ON DELETE CASCADE,
    INDEX idx_case_id (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '案例要素表';

-- 案件要素字典表
CREATE TABLE IF NOT EXISTS case_element_dict (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    element_code    VARCHAR(64) NOT NULL UNIQUE,
    element_name    VARCHAR(128) NOT NULL,
    category        VARCHAR(64),
    sort_order      INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '案件要素字典';

-- --------------------------------------------------
-- 第四部分：搜索与反馈表
-- --------------------------------------------------

-- 搜索日志表
CREATE TABLE IF NOT EXISTS search_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(64) COMMENT '用户ID',
    query_text      VARCHAR(500) NOT NULL,
    intent_type     VARCHAR(32) COMMENT '意图类型',
    result_count    INT DEFAULT 0,
    response_time_ms INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_query (query_text(50)),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '搜索日志表';

-- 搜索反馈表
CREATE TABLE IF NOT EXISTS search_feedback (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    search_log_id   BIGINT NOT NULL,
    article_id      BIGINT COMMENT '反馈的条款ID',
    is_helpful      TINYINT COMMENT '是否有用',
    user_comment    TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (search_log_id) REFERENCES search_log(id),
    FOREIGN KEY (article_id) REFERENCES law_article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '搜索反馈表';

-- 用户反馈表
CREATE TABLE IF NOT EXISTS user_feedback (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT,
    biz_module      VARCHAR(32),
    biz_id          VARCHAR(64),
    feedback_type   VARCHAR(32),
    content         TEXT,
    rating          TINYINT,
    status          TINYINT DEFAULT 0 COMMENT '0待处理 1处理中 2已解决 3已关闭',
    handler_id      BIGINT,
    handle_note     TEXT,
    handled_at      DATETIME,
    sla_due_at      DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module_status (biz_module, status),
    INDEX idx_sla (sla_due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户反馈';

-- --------------------------------------------------
-- 第五部分：知识库与文档表
-- --------------------------------------------------

-- 知识库表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_uuid         VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    owner_id        VARCHAR(64) NOT NULL,
    is_public       TINYINT DEFAULT 0 COMMENT '是否团队共享',
    doc_count       INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id           BIGINT NOT NULL,
    doc_uuid        VARCHAR(64) NOT NULL UNIQUE,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(32) COMMENT 'pdf/word/txt',
    file_size       BIGINT COMMENT '文件大小',
    file_path       VARCHAR(500) COMMENT '存储路径',
    chunk_count     INT DEFAULT 0,
    parse_status    TINYINT DEFAULT 0 COMMENT '0待处理 1解析中 2已完成 3失败',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    INDEX idx_kb (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '文档表';

-- 知识库分块存储表
CREATE TABLE IF NOT EXISTS kb_chunk_store (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id           BIGINT NOT NULL COMMENT '知识库ID',
    file_name       VARCHAR(255) COMMENT '文件名',
    content         MEDIUMTEXT NOT NULL COMMENT '分块内容',
    chunk_index     INT DEFAULT 0 COMMENT '分块序号',
    token_count     INT DEFAULT 0 COMMENT 'token数',
    vector_id       VARCHAR(128) COMMENT 'Milvus向量ID',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    INDEX idx_kb (kb_id),
    INDEX idx_file (kb_id, file_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '知识库文档分块存储';

-- 会话表
CREATE TABLE IF NOT EXISTS kb_session (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_uuid    VARCHAR(64) NOT NULL UNIQUE,
    kb_id           BIGINT NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    title           VARCHAR(200),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '会话表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS kb_chat_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_uuid    VARCHAR(64) NOT NULL COMMENT '会话UUID',
    user_id         VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    role            VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    content         TEXT NOT NULL COMMENT '消息内容',
    `order`         INT DEFAULT 0 COMMENT '消息顺序',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_uuid),
    INDEX idx_user (user_id),
    INDEX idx_user_session (user_id, session_uuid),
    INDEX idx_order (`order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '聊天消息表';

-- 文件问答会话表
CREATE TABLE IF NOT EXISTS qa_session (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_uuid    VARCHAR(64) NOT NULL UNIQUE,
    user_id         VARCHAR(64),
    kb_id           BIGINT,
    title           VARCHAR(200),
    msg_count       INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '文件问答会话';

-- 知识库分块策略表
CREATE TABLE IF NOT EXISTS kb_chunk_strategy (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id           BIGINT,
    chunk_size      INT DEFAULT 512,
    chunk_overlap   INT DEFAULT 64,
    splitter        VARCHAR(32) DEFAULT 'recursive',
    status          TINYINT DEFAULT 1,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '知识库分块策略';

-- --------------------------------------------------
-- 第六部分：文书与合同表
-- --------------------------------------------------

-- 文书模板表
CREATE TABLE IF NOT EXISTS doc_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code   VARCHAR(64) NOT NULL UNIQUE,
    template_name   VARCHAR(128) NOT NULL,
    category        VARCHAR(64),
    schema_json     JSON,
    risk_rules      JSON,
    review_required TINYINT DEFAULT 1,
    status          TINYINT DEFAULT 1,
    version         VARCHAR(16) DEFAULT 'v1.0',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '文书模板';

-- 文书草稿表
CREATE TABLE IF NOT EXISTS doc_draft (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    draft_uuid      VARCHAR(64) NOT NULL UNIQUE,
    template_code   VARCHAR(64),
    user_id         VARCHAR(64),
    title           VARCHAR(200),
    content         MEDIUMTEXT,
    claim_amount    DECIMAL(18,2),
    risk_level      VARCHAR(16),
    review_status   TINYINT DEFAULT 0 COMMENT '0待审 1通过 2驳回',
    reviewer_id     BIGINT,
    review_note     TEXT,
    reviewed_at     DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_template (template_code),
    INDEX idx_review (review_status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '文书草稿';

-- 复核规则表
CREATE TABLE IF NOT EXISTS doc_review_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code   VARCHAR(64) NOT NULL,
    rule_type       VARCHAR(32),
    operator        VARCHAR(8),
    threshold       DECIMAL(18,2),
    trigger_action  VARCHAR(32),
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '复核规则';

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

-- 合同审查规则表
CREATE TABLE IF NOT EXISTS contract_review_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    dimension       VARCHAR(64),
    weight          DECIMAL(5,2),
    threshold_high  INT DEFAULT 70,
    threshold_low   INT DEFAULT 40,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '合同审查规则';

-- PPT文档表
CREATE TABLE IF NOT EXISTS ppt_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    ppt_uuid        VARCHAR(64) NOT NULL UNIQUE COMMENT 'PPT文档UUID',
    title           VARCHAR(500) NOT NULL COMMENT 'PPT标题',
    slides_json     TEXT NOT NULL COMMENT '幻灯片JSON数据',
    template_id     VARCHAR(50) DEFAULT 'legal-blue' COMMENT '模板ID',
    user_id         VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    file_path       VARCHAR(500) COMMENT 'PPTX文件存储路径',
    file_size       BIGINT COMMENT '文件大小',
    status          TINYINT DEFAULT 0 COMMENT '状态：0编辑中 1已生成 2已下载',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'PPT文档表';

-- --------------------------------------------------
-- 第七部分：AI能力表（Prompt、模型、Token）
-- --------------------------------------------------

-- Prompt模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_code     VARCHAR(64) NOT NULL,
    module          VARCHAR(32) NOT NULL,
    scene           VARCHAR(64) NOT NULL,
    version         VARCHAR(16) NOT NULL,
    content         MEDIUMTEXT NOT NULL,
    variables       JSON,
    is_active       TINYINT DEFAULT 0,
    is_gray         TINYINT DEFAULT 0,
    gray_ratio      INT DEFAULT 0,
    gray_teams      JSON,
    adopt_rate      DECIMAL(5,4),
    feedback_score  DECIMAL(3,2),
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code_version (prompt_code, version),
    INDEX idx_module (module),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Prompt 模板';

-- Prompt灰度发布表
CREATE TABLE IF NOT EXISTS prompt_gray_release (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_id       BIGINT NOT NULL,
    from_version    VARCHAR(16),
    to_version      VARCHAR(16) NOT NULL,
    ratio           INT NOT NULL,
    teams           JSON,
    started_at      DATETIME,
    ended_at        DATETIME,
    rollback_reason TEXT,
    operator_id     BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Prompt 灰度发布';

-- LLM模型配置表
CREATE TABLE IF NOT EXISTS llm_model_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_code      VARCHAR(64) NOT NULL UNIQUE,
    model_name      VARCHAR(128) NOT NULL,
    provider        VARCHAR(32),
    endpoint        VARCHAR(500),
    api_key_enc     VARCHAR(500),
    temperature     DECIMAL(3,2) DEFAULT 0.7,
    max_tokens      INT DEFAULT 4096,
    top_p           DECIMAL(3,2) DEFAULT 0.95,
    is_primary      TINYINT DEFAULT 0,
    is_fallback     TINYINT DEFAULT 0,
    health_status   TINYINT DEFAULT 1,
    last_check_at   DATETIME,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'LLM 模型配置';

-- Token用量表
CREATE TABLE IF NOT EXISTS llm_token_usage (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_code      VARCHAR(64),
    module          VARCHAR(32),
    prompt_tokens   INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens    INT DEFAULT 0,
    cost_cny        DECIMAL(10,4) DEFAULT 0,
    biz_id          VARCHAR(64),
    user_id         VARCHAR(64),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module_time (module, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'Token 用量';

-- --------------------------------------------------
-- 第八部分：监控告警表
-- --------------------------------------------------

-- 告警规则表
CREATE TABLE IF NOT EXISTS alert_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name       VARCHAR(200) NOT NULL,
    metric          VARCHAR(64) NOT NULL,
    operator        VARCHAR(8),
    threshold       DECIMAL(18,4),
    duration_sec    INT DEFAULT 60,
    level           TINYINT COMMENT '1P0 2P1 3P2',
    channels        JSON,
    receivers       JSON,
    silence_sec     INT DEFAULT 1800,
    biz_module      VARCHAR(32),
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '告警规则';

-- 告警历史表
CREATE TABLE IF NOT EXISTS alert_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT NOT NULL,
    triggered_at    DATETIME NOT NULL,
    resolved_at     DATETIME,
    level           TINYINT,
    metric_value    DECIMAL(18,4),
    message         TEXT,
    notify_status   TINYINT,
    INDEX idx_rule (rule_id),
    INDEX idx_triggered (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '告警历史';

-- --------------------------------------------------
-- 第九部分：系统配置表
-- --------------------------------------------------

-- 系统参数表
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key      VARCHAR(128) NOT NULL UNIQUE,
    config_value    TEXT,
    config_group    VARCHAR(64),
    value_type      VARCHAR(16),
    remark          VARCHAR(500),
    updated_by      BIGINT,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_group (config_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统参数';

-- 数据字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_type       VARCHAR(64) NOT NULL,
    dict_label      VARCHAR(64) NOT NULL,
    dict_value      VARCHAR(128) NOT NULL,
    sort_order      INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    INDEX idx_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '数据字典';

-- 企业查询API配置表
CREATE TABLE IF NOT EXISTS company_api_config (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_name        VARCHAR(64) NOT NULL,
    provider        VARCHAR(32),
    endpoint        VARCHAR(500),
    api_key_enc     VARCHAR(500),
    monthly_quota   INT,
    used_count      INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '企业查询 API 配置';

-- 爬虫任务表
CREATE TABLE IF NOT EXISTS crawl_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name       VARCHAR(200) NOT NULL,
    source          VARCHAR(32),
    crawl_type      VARCHAR(32),
    target_url      VARCHAR(500),
    cron_expression VARCHAR(64),
    status          TINYINT DEFAULT 0,
    last_run_at     DATETIME,
    next_run_at     DATETIME,
    total_crawled   INT DEFAULT 0,
    success_count   INT DEFAULT 0,
    fail_count      INT DEFAULT 0,
    config          JSON,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '爬虫任务';

-- 爬虫日志表
CREATE TABLE IF NOT EXISTS crawl_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         BIGINT NOT NULL,
    started_at      DATETIME,
    finished_at     DATETIME,
    status          TINYINT,
    crawled_count   INT DEFAULT 0,
    success_count   INT DEFAULT 0,
    fail_count      INT DEFAULT 0,
    error_log       TEXT,
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '爬虫日志';

-- 法律研究任务表
CREATE TABLE IF NOT EXISTS legal_research_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_uuid       VARCHAR(64) NOT NULL UNIQUE,
    user_id         VARCHAR(64),
    topic           VARCHAR(500),
    status          VARCHAR(16),
    report          MEDIUMTEXT,
    sources         JSON,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '法律研究任务';

-- --------------------------------------------------
-- 第十部分：法规分类系统表
-- --------------------------------------------------

-- 法规分类类型表
CREATE TABLE IF NOT EXISTS law_category_type (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code       VARCHAR(50) NOT NULL UNIQUE,
    type_name       VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    sort_order      INT DEFAULT 0,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 法规分类表
CREATE TABLE IF NOT EXISTS law_category (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_type_id BIGINT NOT NULL,
    parent_id       BIGINT DEFAULT NULL,
    category_code   VARCHAR(50) NOT NULL UNIQUE,
    category_name   VARCHAR(200) NOT NULL,
    color           VARCHAR(20),
    sort_order      INT DEFAULT 0,
    status          INT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_type FOREIGN KEY (category_type_id) REFERENCES law_category_type(id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES law_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 法规-分类关联表
CREATE TABLE IF NOT EXISTS law_document_category (
    law_id          BIGINT NOT NULL,
    category_id     BIGINT NOT NULL,
    PRIMARY KEY (law_id, category_id),
    CONSTRAINT fk_law_doc FOREIGN KEY (law_id) REFERENCES law_document(id),
    CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES law_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

