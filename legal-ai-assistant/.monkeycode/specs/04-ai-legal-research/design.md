# AI法律研究模块技术设计规格

Feature Name: 04-ai-legal-research
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

对法律问题进行多维度研究，输出结构化分析报告。

**研究报告结构（六段式）：**

```
一、问题界定
    （一）研究背景
    （二）核心问题
    （三）问题边界
    （四）关键术语定义

二、法律依据
    （一）法律体系概览
    （二）核心法规解读（逐条分析）
    （三）配套规定
    （四）法规时效性

三、学术观点
    （一）学术研究概况
    （二）主要学术流派
    （三）学术争议焦点
    （四）研究前沿动态

四、实务指引
    （一）实务场景分类
    （二）分场景操作指引
    （三）合规检查清单

五、风险提示
    （一）法律风险识别
    （二）风险等级评估
    （三）风险防控建议
    （四）应急处置预案

六、结论建议
    （一）核心结论
    （二）行动建议（分主体）
    （三）研究局限
    （四）后续建议
```

**核心价值：**
- 多维度覆盖：法规 + 判例 + 学术论文三方印证
- 可溯源结论：每项法律结论均标注来源URL
- 结构化输出：信息密度高且可读性强
- 高效生成：5分钟内完成完整研究

---

## 1. 技术架构

```
[用户输入研究问题]
         │
         ▼
[问题解析] → 关键词提取 + 类型识别
         │
         ▼
[多源检索] → 法规ES + 案例ES + 论文（可选）
         │
         ▼
[SSE流式推送] → 实时进度展示
         │
         ▼
[AI生成报告] → 六段式结构化输出
         │
         ▼
[引用溯源] → 每项结论标注来源
         │
         ▼
[返回完整报告]
```

---

## 2. API接口设计

### 2.1 SSE流式研究接口

**GET** `/api/v1/legal-research/tasks/{taskId}/stream`

**SSE响应示例：**

```
data: {"phase":"parse","progress":10,"message":"正在解析研究问题..."}
data: {"phase":"search","progress":30,"message":"检索法律法规...找到12条"}
data: {"phase":"search","progress":50,"message":"检索司法判例...找到8条"}
data: {"phase":"generate","progress":75,"message":"正在生成问题界定章节..."}
data: {"phase":"generate","progress":85,"message":"正在生成法律依据章节..."}
data: {"type":"report_complete","reportId":9876543210}
```

### 2.2 研究请求接口

**POST** `/api/v1/legal-research/research`

**Request：**

```json
{
  "query": "建设工程合同纠纷中的工期延误索赔问题研究",
  "depth": "normal",
  "sources": ["laws", "cases", "papers"]
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reportId": "RPT-2023-001",
    "query": "建设工程合同纠纷中的工期延误索赔问题研究",
    "depth": "normal",
    "sections": [
      {
        "id": 1,
        "title": "一、问题界定",
        "content": "<p>...</p>",
        "citations": []
      }
    ],
    "createdAt": "2026-06-12T10:00:00+08:00"
  }
}
```

---

## 3. 数据模型

### ResearchReport

```java
public class ResearchReport {
    String reportId;
    String query;
    String depth;
    List<ReportSection> sections;
    Long createdAt;
}

public class ReportSection {
    Integer id;
    String title;
    String content;
    List<Citation> citations;
}

public class Citation {
    Integer id;
    String title;
    String url;
    String source;
}
```

---

## 4. 错误码定义

| 错误码 | 说明 |
|--------|------|
| RES_001 | 任务失败 |
| RES_002 | 来源不足 |
| RES_003 | 研究超时 |