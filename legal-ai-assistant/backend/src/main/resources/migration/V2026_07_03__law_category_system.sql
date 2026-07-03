-- Law Category System Migration
-- V2026_07_03__law_category_system.sql

-- Drop tables if exist (for clean re-run)
DROP TABLE IF EXISTS law_document_category;
DROP TABLE IF EXISTS law_category;
DROP TABLE IF EXISTS law_category_type;

-- Create law_category_type table
CREATE TABLE law_category_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE,
    type_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create law_category table
CREATE TABLE law_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_type_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    category_code VARCHAR(50) NOT NULL UNIQUE,
    category_name VARCHAR(200) NOT NULL,
    color VARCHAR(20),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_type FOREIGN KEY (category_type_id) REFERENCES law_category_type(id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES law_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create law_document_category junction table
CREATE TABLE law_document_category (
    law_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (law_id, category_id),
    CONSTRAINT fk_law_doc FOREIGN KEY (law_id) REFERENCES law_document(id),
    CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES law_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add foreign key columns to law_document
ALTER TABLE law_document
    ADD COLUMN category_id_level BIGINT DEFAULT NULL,
    ADD COLUMN category_id_dept BIGINT DEFAULT NULL,
    ADD COLUMN category_id_industry BIGINT DEFAULT NULL,
    ADD COLUMN category_id_custom BIGINT DEFAULT NULL;

-- Insert initial data: 4 category types
INSERT INTO law_category_type (type_code, type_name, description, sort_order) VALUES
('LEVEL', '效力级别', '法律法规的效力等级分类', 1),
('DEPT', '发文机关', '法律法规的发布机关分类', 2),
('INDUSTRY', '行业领域', '法律法规适用的行业领域分类', 3),
('CUSTOM', '自定义分类', '用户自定义的分类维度', 4);

-- Insert initial data: law_category (~16 rows)
-- Level categories (type_code = LEVEL)
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(1, NULL, 'LEVEL_001', '法律', '#FF6B6B', 1, 1),
(1, NULL, 'LEVEL_002', '行政法规', '#4ECDC4', 2, 1),
(1, NULL, 'LEVEL_003', '部门规章', '#45B7D1', 3, 1),
(1, NULL, 'LEVEL_004', '地方性法规', '#96CEB4', 4, 1),
(1, NULL, 'LEVEL_005', '司法解释', '#DDA0DD', 5, 1);

-- Department categories (type_code = DEPT)
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(2, NULL, 'DEPT_001', '全国人民代表大会', '#FF6B6B', 1, 1),
(2, NULL, 'DEPT_002', '国务院', '#4ECDC4', 2, 1),
(2, NULL, 'DEPT_003', '最高人民法院', '#45B7D1', 3, 1),
(2, NULL, 'DEPT_004', '最高人民检察院', '#96CEB4', 4, 1),
(2, NULL, 'DEPT_005', '国务院各部门', '#DDA0DD', 5, 1);

-- Industry categories (type_code = INDUSTRY)
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

-- Custom categories (type_code = CUSTOM)
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order, status) VALUES
(4, NULL, 'CUSTOM_001', '常用收藏', '#FF6B6B', 1, 1),
(4, NULL, 'CUSTOM_002', '待学习', '#4ECDC4', 2, 1),
(4, NULL, 'CUSTOM_003', '工作参考', '#45B7D1', 3, 1);
