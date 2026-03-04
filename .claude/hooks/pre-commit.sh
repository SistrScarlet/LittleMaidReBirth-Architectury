#!/bin/bash
# PreToolUse hook: git commit 前に SpotBugs を実行
# Bash ツールのコマンドに "git commit" が含まれる場合にトリガー

set -euo pipefail

COMMAND=$(echo "$TOOL_INPUT" | jq -r '.command // empty')

# git commit コマンドでなければスキップ
if ! echo "$COMMAND" | grep -qE '(^|\s|&&|\|)git\s+commit'; then
  exit 0
fi

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "[SpotBugs] コミット前にバグチェックを実行中..."
cd "$PROJECT_ROOT"
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew spotbugsMain 2>&1 | tail -10

echo "[SpotBugs] チェック完了"
exit 0
