# 法规文档解析 AI 审查工具设计文档

## 概述

Python 编写的法规文档解析工具，解析 Word (.docx)、PDF、JSON、纯文本格式文件，通过 AI（MiniMax）自动完成结构提取、内容审查和分类标注，最终写入 MySQL `law_document` + `law_article` 表，复用现有 schema。工具以子进程嵌入 Spring Boot，通过 JSON-RPC 2.0 over stdin/stdout 与 Java 通信，同时保留 CLI 模式供独立使用。

## 核心技术选型

- **文件解析**：pdfplumber（PDF）、python-docx（Word）、标准库（JSON/TXT）
- **AI 审查**：MiniMax Chat Completions API（OpenAI 兼容接口），三阶段处理
- **进程通信**：JSON-RPC 2.0 over stdin/stdout（LSP 风格协议）
- **数据库**：pymysql，直写 MySQL `law_document` + `law_article`
- **AI 代理**：python-dotenv（环境变量配置）

## 阶段一：结构提取（Structure Extraction）

输入：原始文档文本
输出：章节树 + 条款列表（未审查）

**提示词策略**：
- 分析文档整体结构（总则、分则、附则、章节）
- 识别条款编号规律（如"第X条"、"第X章"、"第X节"）
- 识别条款内容边界（遇下一条款编号终止）
- 输出标准 JSON 结构

**输出格式**：
```json
{
  "law_title": "中华人民共和国民法典",
  "short_title": "民法典",
  "issuing_authority": "全国人民代表大会",
  "issue_date": "2020-05-28",
  "effective_date": "2021-01-01",
  "document_no": "主席令第四十五号",
  "chapter_tree": [
    {"title": "第一编 总则", "level": 1, "children": [
      {"title": "第一章 基本规定", "level": 2, "children": []}
    ]}
  ],
  "articles": [
    {
      "article_no": "第一条",
      "title": "立法目的和依据",
      "content": "为了保护民事主体的合法权益...",
      "chapter_path": "第一编 总则/第一章 基本规定",
      "sort_order": 1
    }
  ]
}
```

## 阶段二：内容审查（Content Review）

输入：结构提取结果
输出：审查后条款列表

**审查内容**：
- 条款编号连续性检查（缺失编号警告）
- 条款内容格式修正（全角/半角、标点规范化）
- 重复条款检测（内容哈希去重）
- 条款标题补全（从内容首句推断）

**输出格式**：在原 articles 基础上增加 `review_warnings` 字段

## 阶段三：自动分类（Auto Classification）

输入：审查后条款列表 + 法规标题
输出：suggested_categories（与现有 `LawImportPreview.CategorySuggestion` 兼容）

**分类策略**：
- 根据法规名称关键词推断一级分类（如"劳动"→劳动法，"合同"→民法商法）
- 根据条款内容关键词推断二级分类
- 置信度评分（0.0-1.0）

## 数据库写入

### law_document 表字段映射

| 数据库字段 | 来源 | 说明 |
|-----------|------|------|
| law_uuid | 生成的 UUID | 主键 |
| title | AI 提取 | 法规全称 |
| short_title | AI 提取或推断 | 简称 |
| category_l1 | AI 分类 | 一级分类 |
| category_l2 | AI 分类 | 二级分类 |
| issuing_authority | AI 提取 | 发布机关 |
| issue_date | AI 提取 | 发布日期 |
| effective_date | AI 提取或推断 | 生效日期 |
| status | 固定值 1 | 现行有效 |
| source_url | 传入参数 | 来源 URL |
| source_name | 传入参数 | 来源名称 |

### law_article 表字段映射

| 数据库字段 | 来源 | 说明 |
|-----------|------|------|
| law_id | 写入 law_document 后获取 | 外键 |
| article_uuid | 生成的 UUID | 主键 |
| article_no | AI 提取 | 条款编号 |
| title | AI 提取 | 条款标题 |
| content | AI 提取 | 条款正文 |
| content_hash | SHA-256(content) | 去重依据 |
| sort_order | AI 提取 | 排序序号 |

## JSON-RPC 协议

### 通信格式

请求（Java → Python）：
```json
{"jsonrpc": "2.0", "id": 1, "method": "parse", "params": {"file_path": "/data/law.pdf"}}
{"jsonrpc": "2.0", "id": 2, "method": "review", "params": {"structure_result": {...}}}
{"jsonrpc": "2.0", "id": 3, "method": "import", "params": {"law_data": {...}, "dry_run": false}}
{"jsonrpc": "2.0", "id": 4, "method": "shutdown", "params": {}}
```

响应（Python → Java）：
```json
{"jsonrpc": "2.0", "id": 1, "result": {"law_title": "...", "articles": [...]}}
{"jsonrpc": "2.0", "id": 1, "error": {"code": -32600, "message": "Invalid JSON-RPC request"}}
```

### 方法说明

| 方法 | 说明 | 参数 |
|------|------|------|
| `parse` | 解析文件，返回结构提取结果 | `{file_path: str}` |
| `review` | 对已解析结果进行 AI 审查 | `{structure_result: object}` |
| `import` | 写入数据库 | `{law_data: object, dry_run: bool}` |
| `batch` | 批量解析并导入 | `{file_paths: list[str], dry_run: bool}` |
| `shutdown` | 优雅关闭子进程 | `{}` |

## 错误处理

| 错误码 | 说明 |
|--------|------|
| -32600 | Invalid Request（参数格式错误） |
| -32602 | Invalid Params（参数值不合法） |
| -32603 | Internal Error（内部处理错误，如文件读取失败、API 超时） |
| -32001 | Parse Error（文档解析失败） |
| -32002 | AI Review Failed（AI 审查失败） |
| -32003 | Database Error（数据库写入失败） |

## 项目结构

```
law_parser/
├── main.py                    # CLI 入口 + 子进程启动
├── pyproject.toml             # 依赖管理
├── .env.example               # 环境变量示例
├── src/
│   ├── __init__.py
│   ├── parser/
│   │   ├── __init__.py
│   │   ├── base.py           # Parser 基类
│   │   ├── pdf_parser.py     # PDF 解析
│   │   ├── docx_parser.py    # Word 解析
│   │   ├── json_parser.py    # JSON 解析
│   │   └── txt_parser.py     # 纯文本解析
│   ├── ai/
│   │   ├── __init__.py
│   │   ├── client.py         # MiniMax API 客户端
│   │   ├── structure_extractor.py  # 阶段一
│   │   ├── content_reviewer.py    # 阶段二
│   │   └── classifier.py     # 阶段三
│   ├── db/
│   │   ├── __init__.py
│   │   └── writer.py         # MySQL 写入
│   └── protocol/
│       ├── __init__.py
│       ├── jsonrpc.py        # JSON-RPC 2.0 实现
│       └── server.py         # stdin/stdout 服务器
└── tests/
    ├── __init__.py
    ├── test_parser.py
    └── test_ai.py
```

## 环境变量

```
MINIMAX_API_KEY=your_api_key
MINIMAX_BASE_URL=https://api.minimax.chat/v1
MINIMAX_MODEL=MiniMax-M3
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYMAX_PASSWORD=
MYSQL_DATABASE=legal_ai
```

## CLI 使用方式

```bash
# 解析单个文件
python -m law_parser parse /data/law.pdf

# 审查已有解析结果
python -m law_parser review /data/structure.json

# 导入数据库（dry-run）
python -m law_parser import /data/structure.json --dry-run

# 批量导入
python -m law_parser batch /data/laws/*.pdf --dry-run

# 启动 JSON-RPC 服务（由 Spring Boot 调用）
python -m law_parser serve
```

## Java 集成

Spring Boot 通过 `ProcessBuilder` 启动 Python 子进程，建立 stdin/stdout 管道。发送 JSON-RPC 请求后同步读取响应。子进程在 `CommandLineRunner` 或 `@PostConstruct` 中启动，应用关闭时通过 `shutdown` 方法优雅终止。

## 测试策略

- **单元测试**：各 Parser 解析正确性、AI 提示词输出格式验证、数据库写入正确性
- **集成测试**：完整流程测试（文件 → 结构 → 审查 → 入库）
- **Mock**：使用 Mock API Key 测试 AI 调用逻辑

## 已知限制

- PDF 扫描件（图片）需要 OCR 支持，后续扩展
- 手写条款编号格式可能无法识别
- 法规版本更新（替代旧法）需要人工判断
