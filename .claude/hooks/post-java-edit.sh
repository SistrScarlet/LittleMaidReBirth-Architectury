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
GJF_JAR="$PROJECT_ROOT/.claude/tools/google-java-format-1.17.0-all-deps.jar"
CS_JAR="$PROJECT_ROOT/.claude/tools/checkstyle-10.21.4-all.jar"
CS_CONFIG="$PROJECT_ROOT/config/checkstyle/checkstyle.xml"

# 1. google-java-format で整形 (--replace でインプレース)
if [[ -f "$GJF_JAR" ]]; then
  java -jar "$GJF_JAR" --replace "$FILE_PATH" 2>/dev/null || true
fi

# 2. Checkstyle でチェック (警告として出力、終了コードは無視)
if [[ -f "$CS_JAR" && -f "$CS_CONFIG" ]]; then
  RESULT=$(java -jar "$CS_JAR" -c "$CS_CONFIG" "$FILE_PATH" 2>&1 || true)
  # "Starting audit..." と "Audit done." 以外の行があれば警告表示
  WARNINGS=$(echo "$RESULT" | grep -v "^Starting audit" | grep -v "^Audit done" | grep -v "^$" || true)
  if [[ -n "$WARNINGS" ]]; then
    echo "[Checkstyle] $FILE_PATH:"
    echo "$WARNINGS"
  fi
fi

exit 0
