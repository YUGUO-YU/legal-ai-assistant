USE legal_ai;

-- 默认数据

-- 第十一部分：默认数据
-- --------------------------------------------------

-- 后台管理员 (admin / admin123)
-- 密码: SHA-256Hex("admin123") = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
INSERT INTO admin_user (id, username, password, real_name, status) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '超级管理员', 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), real_name = VALUES(real_name), status = VALUES(status);

-- 前端演示用户 (demo / demo123)
-- 密码: SHA-256Hex("demo123") = d3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791
INSERT INTO frontend_user (id, username, password, real_name, email, status, approved) VALUES
('u-001', 'demo', 'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', '演示用户', 'demo@legal-ai.local', 1, 1)
ON DUPLICATE KEY UPDATE password = VALUES(password), real_name = VALUES(real_name), email = VALUES(email), status = VALUES(status), approved = VALUES(approved);

-- 角色数据
INSERT IGNORE INTO admin_role (id, role_code, role_name, data_scope, status, remark) VALUES
(1, 'SUPER_ADMIN', '超级管理员', 4, 1, '全部权限'),
(2, 'OPS_ADMIN', '运营管理员', 4, 1, '基础+数据资产+运营+系统配置'),
(3, 'LEGAL_ADMIN', '法务管理员', 4, 1, '数据资产'),
(4, 'DEV_OPS', '运维工程师', 4, 1, '基础+监控+系统配置+AI域'),
(5, 'AUDITOR', '审计员', 4, 1, '只读 审计+监控+运营'),
(6, 'READONLY', '只读访客', 4, 1, '全部只读');

-- 管理员-角色关联
INSERT IGNORE INTO admin_user_role (user_id, role_id) VALUES (1, 1);

-- 系统参数
INSERT IGNORE INTO sys_config (config_key, config_value, config_group, value_type, remark) VALUES
('llm.default.temperature', '0.3', 'llm', 'number', '默认温度'),
('cache.search.ttl_sec', '300', 'cache', 'number', '搜索缓存 TTL'),
('rate_limit.api.search.qps', '100', 'rate_limit', 'number', '搜索限流'),
('rate_limit.api.draft.qps', '20', 'rate_limit', 'number', '起草限流'),
('feature.hallucination_detect.enabled', 'true', 'feature', 'boolean', '幻觉检测');

-- 数据字典
INSERT IGNORE INTO sys_dict (dict_type, dict_label, dict_value, sort_order) VALUES
('audit_status', '待审核', '0', 1),
('audit_status', '已通过', '1', 2),
('audit_status', '已驳回', '2', 3),
('risk_level', '低', 'low', 1),
('risk_level', '中', 'medium', 2),
('risk_level', '高', 'high', 3),
('llm_provider', 'MiniMax', 'minimax', 1),
('llm_provider', 'OpenAI', 'openai', 2);

-- LLM模型配置
INSERT IGNORE INTO llm_model_config (model_code, model_name, provider, endpoint, is_primary, is_fallback, status) VALUES
('MiniMax-M3', 'MiniMax-M3', 'minimax', 'https://api.minimax.chat/v1', 1, 0, 1),
('MiniMax-M2.7', 'MiniMax-M2.7 (备用)', 'minimax', 'https://api.minimax.chat/v1', 0, 1, 1);

-- 告警规则
INSERT IGNORE INTO alert_rule (rule_name, metric, operator, threshold, duration_sec, level, channels, receivers, biz_module, status) VALUES
('JVM 堆使用率过高', 'jvm.memory.heap.used.pct', '>', 0.85, 300, 2, JSON_ARRAY('feishu'), JSON_ARRAY('ops'), NULL, 1),
('接口 RT P99 > 3s', 'interface.rt.p99', '>', 3000, 180, 2, JSON_ARRAY('feishu'), JSON_ARRAY('ops'), NULL, 1),
('ES 索引缺失', 'es.index.missing', '>', 0, 60, 1, JSON_ARRAY('feishu'), JSON_ARRAY('ops'), NULL, 1),
('Milvus 不可用', 'milvus.collection.unavailable', '>', 0, 60, 1, JSON_ARRAY('feishu'), JSON_ARRAY('ops'), NULL, 1),
('LLM 调用失败率', 'llm.api.fail_rate', '>', 0.05, 120, 1, JSON_ARRAY('feishu'), JSON_ARRAY('ops'), NULL, 1);

-- 后台菜单树
INSERT IGNORE INTO admin_menu (id, parent_id, menu_name, menu_type, path, component, permission, icon, sort_order, biz_module) VALUES
-- 一级目录
(1,  0, '概览',      1, '/admin',                  NULL,                          NULL,             'Odometer',   1, NULL),
(2,  0, '基础设施',   1, '/admin/infra',            NULL,                          NULL,             'Tools',      2, NULL),
(3,  0, '数据资产',   1, '/admin/biz',              NULL,                          NULL,             'Document',   3, NULL),
(4,  0, 'AI 能力',    1, '/admin/ai',               NULL,                          NULL,             'MagicStick', 4, NULL),
(5,  0, '运营分析',   1, '/admin/ops',              NULL,                          NULL,             'DataAnalysis',5,NULL),
(6,  0, '监控告警',   1, '/admin/monitor',          NULL,                          NULL,             'Bell',       6, NULL),
(7,  0, '系统配置',   1, '/admin/sys',              NULL,                          NULL,             'Setting',    7, NULL),
-- 二级菜单
(8,  1, '仪表盘',      2, '/admin',                  'admin/AdminDashboard',   'admin:dashboard',    NULL, 1, NULL),
(9,  2, '用户管理',    2, '/admin/infra/users',       'admin/infra/Users',      'admin:user:list',    NULL, 1, 'MOD-00'),
(10, 2, '角色权限',    2, '/admin/infra/roles',       'admin/infra/Roles',      'admin:role:list',    NULL, 2, 'MOD-00'),
(11, 2, '菜单权限',    2, '/admin/infra/menus',       'admin/infra/Menus',      'admin:menu:list',    NULL, 3, 'MOD-00'),
(12, 2, '操作审计',    2, '/admin/infra/audit',       'admin/infra/AuditLogs',  'admin:audit:list',   NULL, 4, 'MOD-00'),
(13, 2, '服务健康',    2, '/admin/infra/service-health','admin/infra/ServiceHealth','admin:health:view',NULL,5,'MOD-00'),
(14, 2, '前端用户',    2, '/admin/infra/frontend-users', 'admin/infra/FrontendUsers', 'admin:frontend-user:list', NULL, 0, 'MOD-00'),
(15, 3, '法规主数据',  2, '/admin/biz/mod01',         'admin/biz/Mod01Laws',    'admin:mod01:list',   NULL, 1, 'MOD-01'),
(16, 3, '法规修订',    2, '/admin/biz/mod01-revisions','admin/biz/Mod01Revisions','admin:mod01:revision',NULL,2,'MOD-01'),
(17, 3, '爬虫任务',    2, '/admin/biz/mod01-crawl',   'admin/biz/Mod01Crawl',   'admin:mod01:crawl',  NULL, 3, 'MOD-01'),
(18, 3, '案件主数据',  2, '/admin/biz/mod02',         'admin/biz/Mod02Cases',   'admin:mod02:list',   NULL, 4, 'MOD-02'),
(19, 3, '案件要素',    2, '/admin/biz/mod02-elements','admin/biz/Mod02Elements','admin:mod02:element',NULL, 5, 'MOD-02'),
(20, 3, '文书模板',    2, '/admin/biz/mod03-templates','admin/biz/Mod03Templates','admin:mod03:template',NULL,6,'MOD-03'),
(21, 3, '草稿复核',    2, '/admin/biz/mod03-drafts',  'admin/biz/Mod03Drafts',  'admin:mod03:draft',  NULL, 7, 'MOD-03'),
(22, 3, '复核规则',    2, '/admin/biz/mod03-rules',   'admin/biz/Mod03ReviewRules','admin:mod03:rule',NULL,  8, 'MOD-03'),
(23, 3, '研究任务',    2, '/admin/biz/mod04',         'admin/biz/Mod04Tasks',   'admin:mod04:list',   NULL, 9, 'MOD-04'),
(24, 3, '企业 API',    2, '/admin/biz/mod05',         'admin/biz/Mod05CompanyApis','admin:mod05:list', NULL, 10, 'MOD-05'),
(25, 3, '案例查询日志',2, '/admin/biz/mod06',         'admin/biz/Mod06CaseSearch','admin:mod06:list',  NULL, 11, 'MOD-06'),
(26, 3, '法规查询',    2, '/admin/biz/mod07',         'admin/biz/Mod07Laws',    'admin:mod07:list',   NULL, 12, 'MOD-07'),
(27, 3, '合同规则',    2, '/admin/biz/mod08',         'admin/biz/Mod08ContractRules','admin:mod08:list',NULL, 13, 'MOD-08'),
(28, 3, '知识库',      2, '/admin/biz/mod09-kb',      'admin/biz/Mod09KbBases', 'admin:mod09:kb',     NULL, 14, 'MOD-09'),
(29, 3, '分块策略',    2, '/admin/biz/mod09-strategy','admin/biz/Mod09Strategy', 'admin:mod09:strategy',NULL,15,'MOD-09'),
(30, 3, '问答会话',    2, '/admin/biz/mod10',         'admin/biz/Mod10QaSessions','admin:mod10:list',  NULL, 16, 'MOD-10'),
(31, 4, 'Prompt 管理', 2, '/admin/ai/prompts',        'admin/ai/Prompts',       'admin:ai:prompt',    NULL, 1, NULL),
(32, 4, '灰度发布',    2, '/admin/ai/gray',           'admin/ai/GrayReleases',  'admin:ai:gray',      NULL, 2, NULL),
(33, 4, '模型配置',    2, '/admin/ai/llm',            'admin/ai/LlmModels',     'admin:ai:llm',       NULL, 3, NULL),
(34, 4, 'Token 用量',  2, '/admin/ai/token',          'admin/ai/TokenUsage',    'admin:ai:token',     NULL, 4, NULL),
(35, 4, 'Milvus 集合', 2, '/admin/ai/milvus',         'admin/ai/MilvusCollections','admin:ai:milvus', NULL,   5, NULL),
(36, 5, '用户反馈',    2, '/admin/ops/feedback',      'admin/ops/UserFeedback', 'admin:ops:feedback', NULL, 1, NULL),
(37, 5, '搜索日志',    2, '/admin/ops/search-logs',   'admin/ops/SearchLogs',   'admin:ops:search',   NULL, 2, NULL),
(38, 6, '告警规则',    2, '/admin/monitor/rules',     'admin/monitor/AlertRules','admin:monitor:rule', NULL, 1, NULL),
(39, 6, '告警历史',    2, '/admin/monitor/history',   'admin/monitor/AlertHistory','admin:monitor:history',NULL,2,NULL),
(40, 7, '系统参数',    2, '/admin/sys/configs',       'admin/sys/SysConfigs',   'admin:sys:config',   NULL, 1, NULL),
(41, 7, '数据字典',    2, '/admin/sys/dicts',         'admin/sys/SysDicts',     'admin:sys:dict',     NULL, 2, NULL);

-- 角色-菜单授权
-- SUPER_ADMIN (1): 全部菜单
INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),
(1,21),(1,22),(1,23),(1,24),(1,25),(1,26),(1,27),(1,28),(1,29),(1,30),
(1,31),(1,32),(1,33),(1,34),(1,35),(1,36),(1,37),(1,38),(1,39),(1,40),(1,41);

-- OPS_ADMIN (2): 概览+基础+数据资产+运营+系统
INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES
(2,1),(2,2),(2,3),(2,5),(2,7),(2,8),(2,9),(2,10),(2,11),(2,12),
(2,13),(2,14),(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),(2,22),
(2,23),(2,24),(2,25),(2,26),(2,27),(2,28),(2,29),(2,35),(2,36),(2,39),(2,40),(2,41);

-- LEGAL_ADMIN (3): 概览+数据资产
INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES
(3,1),(3,3),(3,8),(3,14),(3,15),(3,16),(3,17),(3,18),(3,19),(3,20),
(3,21),(3,22),(3,23),(3,24),(3,25),(3,26),(3,27),(3,28),(3,29),(3,41);

-- DEV_OPS (4): 概览+基础+AI+监控+系统
INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES
(4,1),(4,2),(4,4),(4,6),(4,7),(4,8),(4,9),(4,10),(4,11),(4,12),
(4,13),(4,30),(4,31),(4,32),(4,33),(4,34),(4,37),(4,38),(4,39),(4,40),(4,41);

-- AUDITOR (5): 概览+审计+监控+运营
INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES
(5,1),(5,8),(5,12),(5,35),(5,36),(5,37),(5,38),(5,41);

-- READONLY (6): 全部只读
INSERT IGNORE INTO admin_role_menu (role_id, menu_id) VALUES
(6,1),(6,2),(6,3),(6,4),(6,5),(6,6),(6,7),(6,8),(6,9),(6,10),
(6,11),(6,12),(6,13),(6,14),(6,15),(6,16),(6,17),(6,18),(6,19),(6,20),
(6,21),(6,22),(6,23),(6,24),(6,25),(6,26),(6,27),(6,28),(6,29),(6,30),
(6,31),(6,32),(6,33),(6,34),(6,35),(6,36),(6,37),(6,38),(6,39),(6,40),(6,41);

-- 示例法规数据
INSERT IGNORE INTO law_document (law_uuid, title, short_title, category_l1, category_l2, issuing_authority, issue_date, effective_date, status, source_url, source_name) VALUES
('LAW-2023-001', '中华人民共和国民法典', '民法典', '法律', '民法', '全国人民代表大会', '2020-05-28', '2021-01-01', 1, 'https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=', '国家法律法规信息库'),
('LAW-2023-002', '中华人民共和国劳动合同法', '劳动合同法', '法律', '劳动法', '全国人民代表大会常务委员会', '2012-12-28', '2013-07-01', 1, 'https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=', '国家法律法规信息库'),
('LAW-2023-003', '中华人民共和国公司法', '公司法', '法律', '商法', '全国人民代表大会常务委员会', '2023-12-29', '2024-07-01', 4, 'https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjMyYTAxNzk3YWIzYzIyYzA2M2I=', '国家法律法规信息库');

-- 示例法规条款
INSERT IGNORE INTO law_article (law_id, article_uuid, article_no, title, content) VALUES
(1, 'ART-2023-001', '第一百四十八条', '欺诈的认定', '一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。'),
(1, 'ART-2023-002', '第一百四十九条', '第三人欺诈', '第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。'),
(1, 'ART-2023-003', '第一百五十条', '欺诈的效力', '一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。'),
(1, 'ART-2023-004', '第五百七十七条', '违约责任', '当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。'),
(1, 'ART-2023-005', '第五百八十四条', '损失赔偿范围', '当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失。');

-- 示例案例数据
INSERT IGNORE INTO legal_case (case_uuid, case_no, title, court, case_type, case_cause, judgment_date, summary, source_url, source_name) VALUES
('CASE-2021-001', '(2021)沪01民终1234号', '某投资公司与张某合同纠纷案', '上海市第一中级人民法院', '民事', '合同纠纷', '2021-09-15', '法院认定被告在签订投资协议时存在欺诈行为，判决撤销合同。', 'https://wenshu.court.gov.cn/', '中国裁判文书网'),
('CASE-2022-001', '(2022)京02民终5678号', '李某与北京某公司劳动争议案', '北京市第二中级人民法院', '民事', '劳动争议', '2022-06-20', '公司违法解除劳动合同，判决支付经济补偿金。', 'https://wenshu.court.gov.cn/', '中国裁判文书网'),
('CASE-2023-001', '(2023)粤01民终9012号', '陈某与广东某公司装饰装修合同纠纷案', '广东省广州市中级人民法院', '民事', '装饰装修合同纠纷', '2023-08-15', '被告擅自变更材料品牌且进度滞后，构成违约。', 'https://wenshu.court.gov.cn/', '中国裁判文书网');

-- 知识库示例数据
INSERT IGNORE INTO kb_knowledge_base (kb_uuid, name, description, owner_id, is_public, doc_count) VALUES
('KB-2024-001', '劳动法法规库', '劳动法律法规及相关案例汇总', 'system', 1, 156),
('KB-2024-002', '合同纠纷案例', '各类合同纠纷案例集', 'system', 1, 89),
('KB-2024-003', '知识产权法规', '知识产权相关法律法规', 'system', 1, 234);

-- Prompt模板
INSERT IGNORE INTO prompt_template (id, prompt_code, module, scene, version, content, variables, is_active, is_gray, gray_ratio, gray_teams, adopt_rate, feedback_score, created_by) VALUES
(1, 'law-search-v1', 'MOD-07', '法规搜索', 'v1.0.0', '你是一名资深法律顾问。请根据以下参考法规，回答用户问题：\n\n参考法规：\n{{laws}}\n\n用户问题：\n{{question}}\n\n请引用具体条款原文。',
 JSON_OBJECT('laws', '参考法规原文', 'question', '用户问题'), 1, 0, 0, NULL, 0.85, 4.2, 1),
(2, 'draft-generation-v1', 'MOD-03', '文书起草', 'v1.0.0', '你是一名专业法律文书撰写师。请根据以下信息起草一份{{doc_type}}：\n\n案件信息：\n{{case_info}}\n\n当事人：\n{{parties}}\n\n请求事项：\n{{claims}}\n\n请严格按照司法文书格式撰写，语言严谨、逻辑清晰。',
 JSON_OBJECT('doc_type', '文书类型', 'case_info', '案件信息', 'parties', '当事人', 'claims', '请求事项'), 1, 0, 0, NULL, 0.78, 3.9, 1),
(3, 'contract-review-v1', 'MOD-08', '合同审查', 'v1.0.0', '你是一名合同审查专家。请审查以下合同条款，识别风险并给出修改建议：\n\n合同摘要：\n{{contract_summary}}\n\n审查重点：\n{{focus_dimensions}}\n\n输出格式：\n1. 风险等级（高/中/低）\n2. 风险描述\n3. 修改建议\n4. 替代条款草案',
 JSON_OBJECT('contract_summary', '合同摘要', 'focus_dimensions', '审查重点维度'), 1, 0, 0, NULL, 0.72, 3.6, 1);

-- 爬虫任务
INSERT IGNORE INTO crawl_task (id, task_name, source, crawl_type, target_url, cron_expression, status, config) VALUES
(1, '最高人民法院公报', 'spc.gov.cn', 'html', 'https://gongbao.court.gov.cn/', '0 0 8 * * 1', 1,
 JSON_OBJECT('headers', JSON_OBJECT('User-Agent', 'LegalBot/1.0'), 'retry', 3, 'timeout_sec', 30)),
(2, '国家法律法规数据库', 'flk.npc.gov.cn', 'api', 'https://flk.npc.gov.cn/api/', '0 0 6 * * *', 1,
 JSON_OBJECT('api_key_env', 'NPC_API_KEY', 'rate_limit_qps', 5, 'timeout_sec', 60));

-- 企业API配置
INSERT IGNORE INTO company_api_config (id, api_name, provider, endpoint, api_key_enc, monthly_quota, used_count, status) VALUES
(1, '天眼查基础信息', 'tianyancha', 'https://api.tianyancha.com/v3/baseinfo', 'enc:xxxxxxxx', 10000, 2340, 1),
(2, '企查查风险扫描', 'qichacha', 'https://api.qichacha.com/risk/v1', 'enc:yyyyyyyy', 5000, 890, 1);

-- 合同审查规则
INSERT IGNORE INTO contract_review_rule (id, dimension, weight, threshold_high, threshold_low, status) VALUES
(1, '风险条款', 0.30, 80, 50, 1),
(2, '责任分配', 0.25, 75, 45, 1),
(3, '期限条款', 0.20, 70, 40, 1),
(4, '付款条件', 0.15, 85, 55, 1),
(5, '保密条款', 0.10, 90, 60, 1);

-- 文书模板
INSERT IGNORE INTO doc_template (id, template_code, template_name, category, schema_json, risk_rules, review_required, version) VALUES
(1, 'complaint-civil', '民事起诉状模板', '起诉文书',
 JSON_OBJECT('sections', JSON_ARRAY('当事人信息', '诉讼请求', '事实与理由', '证据清单')),
 JSON_ARRAY('诉讼请求是否明确', '被告信息是否完整', '管辖权是否正确'), 1, 'v2.1'),
(2, 'defense-civil', '民事答辩状模板', '答辩文书',
 JSON_OBJECT('sections', JSON_ARRAY('答辩人信息', '答辩意见', '事实与理由', '证据反驳')),
 JSON_ARRAY('是否针对诉求逐条答辩', '是否附证据'), 1, 'v2.0'),
(3, 'contract-general', '通用合同模板', '合同文书',
 JSON_OBJECT('sections', JSON_ARRAY('签约方', '合同标的', '付款条款', '违约责任', '争议解决', '附则')),
 JSON_ARRAY('违约责任是否对等', '管辖约定是否合法', '是否有霸王条款'), 1, 'v3.0');

-- 案件要素字典
INSERT IGNORE INTO case_element_dict (id, element_code, element_name, category, sort_order, status) VALUES
(1, 'cause_of_action', '案由', '案件基本信息', 1, 1),
(2, 'court_level', '法院层级', '案件基本信息', 2, 1),
(3, 'judgment_type', '裁判类型', '裁判结果', 3, 1),
(4, 'penalty_range', '量刑幅度', '裁判结果', 4, 1),
(5, 'litigation_amount', '诉讼标的额', '案件信息', 5, 1),
(6, 'party_type', '当事人类型', '案件信息', 6, 1),
(7, 'evidence_type', '证据类型', '证据信息', 7, 1),
(8, 'law_basis', '法律依据', '法律适用', 8, 1);

-- 知识库分块策略
INSERT IGNORE INTO kb_chunk_strategy (id, kb_id, chunk_size, chunk_overlap, splitter, status) VALUES
(1, NULL, 512, 64, 'recursive', 1),
(2, NULL, 1024, 128, 'semantic', 1),
(3, NULL, 256, 32, 'sentence', 1);

-- 系统公告
INSERT IGNORE INTO sys_announcement (id, title, content, type, priority, status, published_at, created_by) VALUES
(1, '法律AI助手系统正式上线', '欢迎使用法律AI助手系统，本系统提供智能法律咨询、文书生成、案件分析等功能。如有疑问请联系管理员。', 1, 0, 1, NOW(), 'admin'),
(2, '系统功能更新通知 v2.1', '本次更新：1) 新增前端用户注册审核功能；2) 优化审计日志查看体验；3) 修复若干已知问题。', 2, 0, 1, NOW(), 'admin'),
(3, '数据备份通知', '系统将于每周日凌晨2:00-6:00进行数据备份，届时系统服务可能短暂中断，请提前做好准备。', 3, 1, 1, NOW(), 'admin'),
(4, '账户安全提醒', '请勿将账户密码告知他人，定期更换密码。如发现异常登录请立即联系管理员。', 4, 1, 1, NOW(), 'admin');

-- 法规分类类型
INSERT INTO law_category_type (type_code, type_name, description, sort_order) VALUES
('LEVEL', '效力级别', '法律法规的效力等级分类', 1),
('DEPT', '发文机关', '法律法规的发布机关分类', 2),
('INDUSTRY', '行业领域', '法律法规适用的行业领域分类', 3),
('CUSTOM', '自定义分类', '用户自定义的分类维度', 4);

-- 效力级别分类
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(1, NULL, 'LEVEL_001', '法律', '#FF6B6B', 1, 1),
(1, NULL, 'LEVEL_002', '行政法规', '#4ECDC4', 2, 1),
(1, NULL, 'LEVEL_003', '部门规章', '#45B7D1', 3, 1),
(1, NULL, 'LEVEL_004', '地方性法规', '#96CEB4', 4, 1),
(1, NULL, 'LEVEL_005', '司法解释', '#DDA0DD', 5, 1);

-- 发文机关分类
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(2, NULL, 'DEPT_001', '全国人民代表大会', '#FF6B6B', 1, 1),
(2, NULL, 'DEPT_002', '国务院', '#4ECDC4', 2, 1),
(2, NULL, 'DEPT_003', '最高人民法院', '#45B7D1', 3, 1),
(2, NULL, 'DEPT_004', '最高人民检察院', '#96CEB4', 4, 1),
(2, NULL, 'DEPT_005', '国务院各部门', '#DDA0DD', 5, 1);

-- 行业领域分类
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(3, NULL, 'IND_001', '公司法', '#FF6B6B', 1, 1),
(3, NULL, 'IND_002', '劳动法', '#4ECDC4', 2, 1),
(3, NULL, 'IND_003', '知识产权', '#45B7D1', 3, 1),
(3, NULL, 'IND_004', '婚姻家庭', '#96CEB4', 4, 1),
(3, NULL, 'IND_005', '刑事', '#DDA0DD', 5, 1),
(3, NULL, 'IND_006', '民事', '#FFEAA7', 6, 1),
(3, NULL, 'IND_007', '合同法', '#98D8C8', 7, 1),
(3, NULL, 'IND_008', '税法', '#F7DC6F', 8, 1),
(3, NULL, 'IND_009', '金融法', '#BB8FCE', 9, 1),
(3, NULL, 'IND_010', '房地产与建设工程', '#85C1E9', 10, 1),
(3, NULL, 'IND_011', '海关与外贸', '#F0B27A', 11, 1),
(3, NULL, 'IND_012', '环境保护', '#82E0AA', 12, 1),
(3, NULL, 'IND_013', '交通安全', '#F1948A', 13, 1),
(3, NULL, 'IND_014', '食品安全与药品', '#AED6F1', 14, 1),
(3, NULL, 'IND_015', '医药卫生', '#D7BDE2', 15, 1),
(3, NULL, 'IND_016', '教育文化', '#FDEBD0', 16, 1),
(3, NULL, 'IND_017', '招投标与采购', '#D5DBDB', 17, 1),
(3, NULL, 'IND_018', '外商投资与外资', '#A9CCE3', 18, 1),
(3, NULL, 'IND_019', '网络安全与数据保护', '#A3E4D7', 19, 1),
(3, NULL, 'IND_020', '行政法', '#FAD7A0', 20, 1);

-- 自定义分类
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(4, NULL, 'CUSTOM_001', '常用收藏', '#FF6B6B', 1, 1),
(4, NULL, 'CUSTOM_002', '待学习', '#4ECDC4', 2, 1),
(4, NULL, 'CUSTOM_003', '工作参考', '#45B7D1', 3, 1);

-- --------------------------------------------------
-- 完成验证
-- --------------------------------------------------
SELECT '========================================' AS '';
SELECT '  法律AI助手数据库安装完成!' AS '';
SELECT '========================================' AS '';
SELECT '' AS '';
SELECT '默认账号:' AS '';
SELECT '  后台管理: admin / admin123' AS '';
SELECT '  前端演示: demo / demo123' AS '';
SELECT '' AS '';
SELECT '表数量:' AS '';
SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema = 'legal_ai';
