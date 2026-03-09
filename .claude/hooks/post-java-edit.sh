#!/bin/bash
# PostToolUse hook: Edit/Write で .java ファイルが変更された後に実行
# 1. google-java-format で自動整形
# 2. Checkstyle でスタイルチェック (警告のみ)

set -euo pipefail

# stdin から JSON を読み取り、tool_input.file_path を取得
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# .java ファイルでなければスキップ
if [[ -z "$FILE_PATH" || "$FILE_PATH" != *.java ]]; then
  exit 0
fi

# ファイルが存在しなければスキップ
if [[ ! -f "$FILE_PATH" ]]; then
  exit 0
fi

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TOOLS_DIR="$PROJECT_ROOT/.claude/tools"
CS_CONFIG="$PROJECT_ROOT/config/checkstyle/checkstyle.xml"

# .claude/tools/ 内から JAR を検索（バージョン非依存）
GJF_JAR=$(compgen -G "$TOOLS_DIR/google-java-format-*-all-deps.jar" | head -1)
CS_JAR=$(compgen -G "$TOOLS_DIR/checkstyle-*-all.jar" | head -1)

# 1. google-java-format で整形 (--replace でインプレース)
if [[ -n "$GJF_JAR" && -f "$GJF_JAR" ]]; then
  java -jar "$GJF_JAR" --replace "$FILE_PATH" 2>/dev/null || true
fi

# 2. Checkstyle でチェック (警告として出力、終了コードは無視)
if [[ -n "$CS_JAR" && -f "$CS_JAR" && -f "$CS_CONFIG" ]]; then
  RESULT=$(java -jar "$CS_JAR" -c "$CS_CONFIG" "$FILE_PATH" 2>&1 || true)
  # "Starting audit..." と "Audit done." 以外の行があれば警告表示
  WARNINGS=$(echo "$RESULT" | grep -v "^Starting audit" | grep -v "^Audit done" | grep -v "^$" || true)
  if [[ -n "$WARNINGS" ]]; then
    echo "[Checkstyle] $FILE_PATH:"
    echo "$WARNINGS"
  fi
fi

exit 0
