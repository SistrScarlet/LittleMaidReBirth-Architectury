#!/usr/bin/env bash
# jar-search.sh - 依存 jar 内のクラス/メソッドを一括検索するヘルパー
#
# Usage:
#   jar-search.sh find <pattern> [--dir <subdir>] [--all]
#   jar-search.sh grep <jar-pattern> <pattern> [N]
#   jar-search.sh read <jar-pattern> <class-path>
#   jar-search.sh list [filter] [--all]
#
# Global options:
#   --all   バージョンフィルタを解除し、全ソース jar を対象にする
#
# Version filtering:
#   デフォルトでは gradle.properties の minecraft_version を読み、
#   パス内にバージョン文字列を含む jar のみを対象にする。
#   命名規則が変わった場合は --all で解除すること。
#
# Examples:
#   jar-search.sh find FakePlayer
#   jar-search.sh find TestContext --dir minecraftMaven
#   jar-search.sh find SomeClass --all
#   jar-search.sh grep fabric-events-interaction "class FakePlayer"
#   jar-search.sh grep minecraft-merged-@common "getWorld" 10
#   jar-search.sh read fabric-events-interaction net/fabricmc/fabric/api/entity/FakePlayer.java
#   jar-search.sh list fabric
#   jar-search.sh list --all

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOOM_CACHE="$PROJECT_ROOT/.gradle/loom-cache"

# gradle.properties から minecraft_version を読む
MC_VERSION=$(grep "^minecraft_version=" "$PROJECT_ROOT/gradle.properties" | cut -d= -f2)
# 1.20.1 → 1_20_1（パス内のバージョン文字列形式）
MC_VERSION_UNDERSCORE=$(echo "$MC_VERSION" | tr '.' '_')

# グローバルフラグ
SKIP_VERSION_FILTER=false

find_source_jars() {
  local dir_filter="${1:-}"
  local search_path="$LOOM_CACHE"
  if [ -n "$dir_filter" ]; then
    search_path="$LOOM_CACHE/$dir_filter"
    if [ ! -d "$search_path" ]; then
      echo "Error: Directory '$search_path' does not exist" >&2
      echo "Available directories:" >&2
      ls -1 "$LOOM_CACHE" >&2
      return 1
    fi
  fi
  if [ "$SKIP_VERSION_FILTER" = true ]; then
    find "$search_path" -name "*-sources.jar" -type f 2>/dev/null
  else
    find "$search_path" -name "*-sources.jar" -type f 2>/dev/null \
      | grep "$MC_VERSION_UNDERSCORE\|$MC_VERSION"
  fi
}

resolve_jar() {
  local jar_pattern="$1"
  local jars
  jars=$(find_source_jars | grep -i "$jar_pattern" || true)
  local count
  count=$(echo "$jars" | grep -c . || true)

  if [ "$count" -eq 0 ] || [ -z "$jars" ]; then
    echo "Error: No source jar matching '$jar_pattern' found" >&2
    echo "Use 'jar-search.sh list' to see available jars" >&2
    return 1
  elif [ "$count" -gt 1 ]; then
    echo "Error: Multiple jars match '$jar_pattern' ($count matches):" >&2
    echo "$jars" | while IFS= read -r j; do
      echo "  ${j#$PROJECT_ROOT/}" >&2
    done
    echo "Narrow your pattern to match exactly one jar" >&2
    return 1
  fi
  echo "$jars"
}

version_label() {
  if [ "$SKIP_VERSION_FILTER" = true ]; then
    echo "all versions"
  else
    echo "MC $MC_VERSION"
  fi
}

cmd_find() {
  local pattern=""
  local dir_filter=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --dir) dir_filter="$2"; shift 2 ;;
      --all) SKIP_VERSION_FILTER=true; shift ;;
      *) pattern="$1"; shift ;;
    esac
  done

  if [ -z "$pattern" ]; then
    echo "Usage: jar-search.sh find <pattern> [--dir <subdir>] [--all]" >&2
    return 1
  fi

  echo "=== Searching for '$pattern' in source jars ($(version_label)) ==="
  [ -n "$dir_filter" ] && echo "    (directory: $dir_filter)"
  local found=0
  while IFS= read -r jar; do
    local matches
    matches=$(unzip -l "$jar" 2>/dev/null | grep -i "$pattern" || true)
    if [ -n "$matches" ]; then
      local jar_short="${jar#$PROJECT_ROOT/}"
      echo ""
      echo "--- $jar_short ---"
      echo "$matches"
      found=1
    fi
  done < <(find_source_jars "$dir_filter")
  if [ "$found" -eq 0 ]; then
    echo "(no matches found)"
  fi
}

cmd_grep() {
  local jar_pattern="${1:?Usage: jar-search.sh grep <jar-pattern> <pattern> [max_matches]}"
  local pattern="${2:?Usage: jar-search.sh grep <jar-pattern> <pattern> [max_matches]}"
  local max_matches="${3:-15}"

  local jar
  jar=$(resolve_jar "$jar_pattern") || return 1
  local jar_short="${jar#$PROJECT_ROOT/}"

  echo "=== Grepping '$pattern' in $jar_short (max $max_matches per file) ==="

  local file_list
  file_list=$(unzip -l "$jar" 2>/dev/null | awk '{print $NF}' | grep '\.java$' || true)
  local total_hits=0

  while IFS= read -r file; do
    [ -z "$file" ] && continue
    local hits
    hits=$(unzip -p "$jar" "$file" 2>/dev/null | grep -n "$pattern" | head -"$max_matches" || true)
    if [ -n "$hits" ]; then
      echo ""
      echo "  $file"
      echo "$hits" | sed 's/^/    /'
      total_hits=$((total_hits + 1))
    fi
  done <<< "$file_list"

  if [ "$total_hits" -eq 0 ]; then
    echo "(no matches found)"
  fi
}

cmd_read() {
  local jar_pattern="${1:?Usage: jar-search.sh read <jar-pattern> <class-path>}"
  local class_path="${2:?Usage: jar-search.sh read <jar-pattern> <class-path>}"

  local jar
  jar=$(resolve_jar "$jar_pattern") || return 1
  local jar_short="${jar#$PROJECT_ROOT/}"

  echo "=== Reading $class_path from $jar_short ==="
  unzip -p "$jar" "$class_path" 2>/dev/null || {
    echo "Error: '$class_path' not found in jar" >&2
    echo "Use 'jar-search.sh find <ClassName>' to locate the correct path" >&2
    return 1
  }
}

cmd_list() {
  local filter=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --all) SKIP_VERSION_FILTER=true; shift ;;
      *) filter="$1"; shift ;;
    esac
  done

  echo "=== Source jars ($(version_label)) ==="
  find_source_jars | while IFS= read -r jar; do
    local jar_short="${jar#$PROJECT_ROOT/}"
    if [ -z "$filter" ] || echo "$jar_short" | grep -qi "$filter"; then
      echo "  $jar_short"
    fi
  done
}

case "${1:-help}" in
  find)  shift; cmd_find "$@" ;;
  grep)  shift; cmd_grep "$@" ;;
  read)  shift; cmd_read "$@" ;;
  list)  shift; cmd_list "${@}" ;;
  *)
    cat <<'USAGE'
Usage:
  jar-search.sh find <pattern> [--dir <subdir>] [--all]
  jar-search.sh grep <jar-pattern> <pattern> [N]
  jar-search.sh read <jar-pattern> <class-path>
  jar-search.sh list [filter] [--all]

Global options:
  --all   Disable version filter (include all MC versions in loom-cache)

Version filtering:
  By default, filters jars by minecraft_version from gradle.properties.
  Use --all if jar naming conventions change or to debug version issues.

Examples:
  jar-search.sh find FakePlayer                   # Search all jars for current MC version
  jar-search.sh find TestContext --dir minecraftMaven  # Search Minecraft jars only
  jar-search.sh find SomeClass --all              # Search all versions
  jar-search.sh grep fabric-events-interaction "class FakePlayer"
  jar-search.sh grep minecraft-merged-@common "getWorld" 10
  jar-search.sh read fabric-events-interaction net/fabricmc/fabric/api/entity/FakePlayer.java
  jar-search.sh list fabric                       # List Fabric-related jars
  jar-search.sh list --all                        # List all jars (no version filter)
USAGE
    ;;
esac
