#!/usr/bin/env bash
# auto-commit-push.sh
# 自动 stage 改动，生成提交信息，push 到 origin/main。
# 用法:
#   ./scripts/auto-commit-push.sh "feat: 你的标题"        # 指定标题
#   ./scripts/auto-commit-push.sh                        # 让 AI 生成标题（stdin 提供 body）
#   echo "body" | ./scripts/auto-commit-push.sh "标题"    # 提供标题 + body

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "$REPO_ROOT" ]; then
  echo "[auto-commit-push] 未在 git 仓库内运行，跳过" >&2
  exit 0
fi

cd "$REPO_ROOT"

# 1. 检查是否有变更
if git diff --quiet HEAD 2>/dev/null && [ -z "$(git ls-files --others --exclude-standard)" ]; then
  echo "[auto-commit-push] 工作树干净，无变更需要提交"
  exit 0
fi

# 2. Stage 所有变更
git add -A

# 3. 准备 commit 信息
TITLE="${1:-}"
BODY="${2:-}"
if [ -z "$TITLE" ]; then
  # 兜底标题（AI 应该在调脚本时把标题传进来；如果没有就用 diff 文件名）
  TITLE="chore: auto-save at $(date +%Y-%m-%dT%H:%M:%S%z)"
fi

if [ -n "$BODY" ]; then
  COMMIT_MSG="${TITLE}

${BODY}"
else
  COMMIT_MSG="$TITLE"
fi

# 4. 提交
git commit -m "$COMMIT_MSG" >/dev/null
COMMIT_SHA=$(git rev-parse --short HEAD)
echo "[auto-commit-push] 已提交 ${COMMIT_SHA}: ${TITLE}"

# 5. 推送到 origin/main
if git push origin main 2>&1; then
  echo "[auto-commit-push] 推送成功 -> origin/main"
  exit 0
else
  PUSH_EXIT=$?
  echo "[auto-commit-push] 推送失败 (exit=$PUSH_EXIT)，请检查网络/冲突/权限" >&2
  exit $PUSH_EXIT
fi
