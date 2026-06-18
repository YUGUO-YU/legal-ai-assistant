# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- Date: [YYYY-MM-DD]
- Context: [提及的场景或时间]
- Instructions:
  - [用户教导或指示的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [运维部署|构建方法|测试方法|排错调试|工作流协作|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息
- 这有助于避免冗余条目，保持记忆文件整洁

## 条目

[自动上传到 GitHub]
- Date: 2026-06-18
- Context: 用户要求"每次开发完自动上传到 github"
- Instructions:
  - 每次工具调用后，若工作树有变更，自动 stage、生成 conventional commit 信息、push 到 origin/main
  - 提交信息由 AI 根据 git diff 内容总结生成
  - 推送失败（冲突/网络/认证）立即停下来报告，不重试
  - 远程仓库: https://github.com/YUGUO-YU/legal-ai-assistant (branch: main)
  - 助手脚本: scripts/auto-commit-push.sh
