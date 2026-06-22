-- 法律AI助手系统数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS legal_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE legal_ai;

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
    source_name VARCHAR(100) COMMENT '来源名称',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_l1, category_l2),
    INDEX idx_status (status),
    INDEX idx_issue_date (issue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规主表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规条款表';

-- 判例表
CREATE TABLE IF NOT EXISTS legal_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_uuid       VARCHAR(64) NOT NULL UNIQUE,
    case_no        VARCHAR(64) COMMENT '案号',
    title          VARCHAR(500) NOT NULL,
    court VARCHAR(128) COMMENT '法院名称',
    case_type      VARCHAR(32) COMMENT '案件类型',
    case_cause     VARCHAR(128) COMMENT '案由',
    judgment_date  DATE COMMENT '裁判日期',
    summary        TEXT COMMENT '裁判摘要',
    full_text_url  VARCHAR(500) COMMENT '全文URL',
    source_url     VARCHAR(500) COMMENT '来源URL',
    source_name VARCHAR(100) COMMENT '来源名称',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type),
    INDEX idx_court (court),
    INDEX idx_judgment_date (judgment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='判例表';

-- 案例主表
CREATE TABLE IF NOT EXISTS tb_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_uuid       VARCHAR(64) NOT NULL UNIQUE,
    case_no         VARCHAR(64) COMMENT '案号',
    case_name       VARCHAR(500) NOT NULL,
    case_type       TINYINT COMMENT '案件类型：1民事 2刑事 3行政',
    case_cause      VARCHAR(128) COMMENT '案由',
    court_level TINYINT COMMENT '法院层级：1最高院 2高院 3中院 4基层',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='案例主表';

-- 案例要素表
CREATE TABLE IF NOT EXISTS tb_case_element (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id         BIGINT NOT NULL,
    element_type   VARCHAR(32) NOT NULL COMMENT '要素类型',
    element_key     VARCHAR(128) COMMENT '要素key',
    element_value   TEXT NOT NULL COMMENT '要素值',
    importance      DECIMAL(3,2) DEFAULT 1.0 COMMENT '重要程度权重',
    FOREIGN KEY (case_id) REFERENCES tb_case(id) ON DELETE CASCADE,
    INDEX idx_case_id (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='案例要素表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规导入历史表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索日志表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索反馈表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规关联表';

-- 知识库表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_uuid VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description TEXT,
    owner_id VARCHAR(64) NOT NULL,
    is_public TINYINT DEFAULT 0 COMMENT '是否团队共享',
    doc_count       INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id BIGINT NOT NULL,
    doc_uuid VARCHAR(64) NOT NULL UNIQUE,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(32) COMMENT 'pdf/word/txt',
    file_size       BIGINT COMMENT '文件大小',
    file_path       VARCHAR(500) COMMENT '存储路径',
    chunk_count INT DEFAULT 0,
    parse_status    TINYINT DEFAULT 0 COMMENT '0待处理 1解析中 2已完成 3失败',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    INDEX idx_kb (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 会话表
CREATE TABLE IF NOT EXISTS kb_session (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_uuid VARCHAR(64) NOT NULL UNIQUE,
    kb_id           BIGINT NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    title           VARCHAR(200),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES kb_knowledge_base(id) ON DELETE CASCADE,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

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
    INDEX idx_order (`order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- 初始化示例数据
INSERT INTO law_document (law_uuid, title, short_title, category_l1, category_l2, issuing_authority, issue_date, effective_date, status, source_url, source_name) VALUES
('LAW-2023-001', '中华人民共和国民法典', '民法典', '法律', '民法', '全国人民代表大会', '2020-05-28', '2021-01-01', 1, 'https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=', '国家法律法规信息库'),
('LAW-2023-002', '中华人民共和国劳动合同法', '劳动合同法', '法律', '劳动法', '全国人民代表大会常务委员会', '2012-12-28', '2013-07-01', 1, 'https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=', '国家法律法规信息库'),
('LAW-2023-003', '中华人民共和国公司法', '公司法', '法律', '商法', '全国人民代表大会常务委员会', '2023-12-29', '2024-07-01', 4, 'https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=', '国家法律法规信息库');

INSERT INTO law_article (law_id, article_uuid, article_no, title, content) VALUES
(1, 'ART-2023-001', '第一百四十八条', '欺诈的认定', '一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。'),
(1, 'ART-2023-002', '第一百四十九条', '第三人欺诈', '第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。'),
(1, 'ART-2023-003', '第一百五十条', '欺诈的效力', '一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。'),
(1, 'ART-2023-004', '第五百七十七条', '违约责任', '当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。'),
(1, 'ART-2023-005', '第五百八十四条', '损失赔偿范围', '当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失。');

INSERT INTO legal_case (case_uuid, case_no, title, court, case_type, case_cause, judgment_date, summary, source_url, source_name) VALUES
('CASE-2021-001', '(2021)沪01民终1234号', '某投资公司与张某合同纠纷案', '上海市第一中级人民法院', '民事', '合同纠纷', '2021-09-15', '法院认定被告在签订投资协议时存在欺诈行为，判决撤销合同。', 'https://wenshu.court.gov.cn/', '中国裁判文书网'),
('CASE-2022-001', '(2022)京02民终5678号', '李某与北京某公司劳动争议案', '北京市第二中级人民法院', '民事', '劳动争议', '2022-06-20', '公司违法解除劳动合同，判决支付经济补偿金。', 'https://wenshu.court.gov.cn/', '中国裁判文书网'),
('CASE-2023-001', '(2023)粤01民终9012号', '陈某与广东某公司装饰装修合同纠纷案', '广东省广州市中级人民法院', '民事', '装饰装修合同纠纷', '2023-08-15', '被告擅自变更材料品牌且进度滞后，构成违约。', 'https://wenshu.court.gov.cn/', '中国裁判文书网');

INSERT INTO kb_knowledge_base (kb_uuid, name, description, owner_id, is_public, doc_count) VALUES
('KB-2024-001', '劳动法法规库', '劳动法律法规及相关案例汇总', 'system', 1, 156),
('KB-2024-002', '合同纠纷案例', '各类合同纠纷案例集', 'system', 1, 89),
('KB-2024-003', '知识产权法规', '知识产权相关法律法规', 'system', 1, 234);

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PPT文档表';