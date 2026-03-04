#!/bin/bash
# PreToolUse hook: git commit 前に SpotBugs を実行
# Bash ツールのコマンドに "git commit" が含まれる場合にトリガー

set -euo pipefail

# stdin から JSON を読み取り、tool_input.command を取得
INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty')

# git commit コマンドでなければスキップ
if ! echo "$COMMAND" | grep -qE '(^|\s|&&|\|)git\s+commit'; then
  exit 0
fi

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "[SpotBugs] コミット前にバグチェックを実行中..."
cd "$PROJECT_ROOT"
./gradlew spotbugsMain 2>&1 | tail -10

echo "[Test] ユニットテストを実行中..."
./gradlew :common:test 2>&1 | tail -10

echo "[Check] チェック完了"
exit 0
