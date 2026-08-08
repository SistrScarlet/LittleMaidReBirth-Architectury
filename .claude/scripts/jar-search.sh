#!/usr/bin/env bash
# jar-search.sh - 依存 jar 内のクラス/メソッドを一括検索するヘルパー
#
# Usage:
#   jar-search.sh find <pattern> [--dir <subdir>] [--all]
#   jar-search.sh grep <jar-pattern> <pattern> [N]
#   jar-search.sh read <jar-pattern> <class-path>
#   jar-search.sh list [filter] [--all]
#   jar-search.sh sig <Class> [--all]
#   jar-search.sh bytecode <Class> [method-pattern] [--all]
#   jar-search.sh apidiff <Class> <old-version> [new-version]
#   jar-search.sh callers <Class> [method] [--dir <subdir>] [--all]
#   jar-search.sh doctor
#
# Global options:
#   --all   バージョンフィルタを解除し、全ソース jar を対象にする
#
# Version filtering:
#   デフォルトでは gradle.properties の minecraft_version を読み、
#   パス内にバージョン文字列を含む jar のみを対象にする。
#   命名規則が変わった場合は --all で解除すること。
#
# sig / bytecode / apidiff はソース jar 不要（コンパイル済み jar を javap で読む）。
# genSources 未実行のバージョンでも API 調査ができる。
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
#   jar-search.sh sig Sound                        # メソッド/フィールドのシグネチャ一覧
#   jar-search.sh sig net.minecraft.client.sound.Sound
#   jar-search.sh bytecode Sound getLocation       # 特定メソッドのバイトコード
#   jar-search.sh bytecode Sound "static"          # static イニシャライザ（定数の初期化を確認）
#   jar-search.sh apidiff Sound 1.20.1             # 1.20.1 → 現バージョンのシグネチャ diff
#   jar-search.sh doctor                           # ソース jar / javap の状態確認

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

# ---- コンパイル済み jar の javap 系サブコマンド ----
# ソース jar が未生成（genSources 未実行）でも API 調査できるようにする。

resolve_javap() {
  if command -v javap >/dev/null 2>&1; then
    echo "javap"
    return 0
  fi
  local jh
  jh=$(grep "^org.gradle.java.home=" "$HOME/.gradle/gradle.properties" 2>/dev/null | cut -d= -f2- || true)
  if [ -n "$jh" ] && [ -x "$jh/bin/javap" ]; then
    echo "$jh/bin/javap"
    return 0
  fi
  echo "Error: javap not found (PATH にも org.gradle.java.home にも無い)" >&2
  return 1
}

find_class_jars() {
  # $1: バージョン指定（省略時は現在の MC バージョン。SKIP_VERSION_FILTER=true なら全 jar）
  local version="${1:-}"
  if [ -n "$version" ]; then
    local version_underscore
    version_underscore=$(echo "$version" | tr '.' '_')
    find "$LOOM_CACHE" -name "*.jar" ! -name "*-sources.jar" -type f 2>/dev/null \
      | grep "$version_underscore\|$version" || true
  elif [ "$SKIP_VERSION_FILTER" = true ]; then
    find "$LOOM_CACHE" -name "*.jar" ! -name "*-sources.jar" -type f 2>/dev/null
  else
    find "$LOOM_CACHE" -name "*.jar" ! -name "*-sources.jar" -type f 2>/dev/null \
      | grep "$MC_VERSION_UNDERSCORE\|$MC_VERSION" || true
  fi
}

resolve_class_entry() {
  # クラス名（Simple / FQCN / パス形式）を "<jar>:<entry>" に解決する。
  # 複数 jar にマッチした場合は最初の 1 つを使い、残りを stderr に通知。
  # $1: クラスパターン, $2: バージョン指定（省略可）
  local pattern="$1" version="${2:-}"
  local path_pattern
  path_pattern=$(echo "$pattern" | tr '.' '/')
  local entry_regex
  if echo "$path_pattern" | grep -q '/'; then
    entry_regex="(^|/)$(echo "$path_pattern" | sed 's/[$]/\\$/g')\.class$"
  else
    entry_regex="(^|/)$(echo "$pattern" | sed 's/[$]/\\$/g')\.class$"
  fi

  local found=""
  local extra_jars=""
  while IFS= read -r jar; do
    [ -z "$jar" ] && continue
    local entry
    entry=$(unzip -Z1 "$jar" 2>/dev/null | grep -E "$entry_regex" | head -1 || true)
    if [ -n "$entry" ]; then
      if [ -z "$found" ]; then
        found="$jar:$entry"
      else
        extra_jars="$extra_jars  ${jar#$PROJECT_ROOT/}\n"
      fi
    fi
  done < <(find_class_jars "$version")

  if [ -z "$found" ]; then
    echo "Error: No compiled class matching '$pattern' found${version:+ (version $version)}" >&2
    echo "Hint: FQCN (net.minecraft.client.sound.Sound) か 'jar-search.sh doctor' で状態確認" >&2
    return 1
  fi
  if [ -n "$extra_jars" ]; then
    echo "Note: 他の jar にも同名クラスあり（最初のマッチを使用）:" >&2
    printf "%b" "$extra_jars" >&2
  fi
  echo "$found"
}

javap_class() {
  # $1: javap フラグ（スペース区切り）, $2: jar, $3: entry
  local flags="$1" jar="$2" entry="$3"
  local javap_bin
  javap_bin=$(resolve_javap) || return 1
  local tmp
  tmp=$(mktemp -d)
  # インナークラスも一緒に展開（javap -c が参照するため）
  local base="${entry%.class}"
  unzip -o -q "$jar" "$entry" "${base}\$*.class" -d "$tmp" 2>/dev/null || true
  if [ ! -f "$tmp/$entry" ]; then
    unzip -o -q "$jar" "$entry" -d "$tmp" 2>/dev/null || true
  fi
  if [ ! -f "$tmp/$entry" ]; then
    echo "Error: failed to extract $entry from $jar" >&2
    rm -rf "$tmp"
    return 1
  fi
  local fqcn
  fqcn=$(echo "$base" | tr '/' '.')
  # shellcheck disable=SC2086
  "$javap_bin" $flags -classpath "$tmp" "$fqcn"
  local rc=$?
  rm -rf "$tmp"
  return $rc
}

cmd_sig() {
  local pattern=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --all) SKIP_VERSION_FILTER=true; shift ;;
      *) pattern="$1"; shift ;;
    esac
  done
  if [ -z "$pattern" ]; then
    echo "Usage: jar-search.sh sig <Class> [--all]" >&2
    return 1
  fi

  local resolved jar entry
  resolved=$(resolve_class_entry "$pattern") || return 1
  jar="${resolved%%:*}"
  entry="${resolved#*:}"
  echo "=== Signatures: $entry ==="
  echo "    (jar: ${jar#$PROJECT_ROOT/})"
  javap_class "-p" "$jar" "$entry"
}

cmd_bytecode() {
  local pattern="" method=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --all) SKIP_VERSION_FILTER=true; shift ;;
      *)
        if [ -z "$pattern" ]; then pattern="$1"; else method="$1"; fi
        shift ;;
    esac
  done
  if [ -z "$pattern" ]; then
    echo "Usage: jar-search.sh bytecode <Class> [method-pattern] [--all]" >&2
    return 1
  fi

  local resolved jar entry
  resolved=$(resolve_class_entry "$pattern") || return 1
  jar="${resolved%%:*}"
  entry="${resolved#*:}"
  echo "=== Bytecode: $entry${method:+ (filter: $method)} ==="
  echo "    (jar: ${jar#$PROJECT_ROOT/})"
  local out
  out=$(javap_class "-p -c" "$jar" "$entry") || return 1
  if [ -n "$method" ]; then
    # javap はメンバ間を空行で区切るため、段落単位でフィルタする
    local filtered
    filtered=$(echo "$out" | awk -v pat="$method" 'BEGIN{RS=""; ORS="\n\n"} $0 ~ pat')
    if [ -z "$filtered" ]; then
      echo "(no member matching '$method' — メンバ一覧は 'sig' で確認)"
    else
      echo "$filtered"
    fi
  else
    echo "$out"
  fi
}

signature_lines() {
  # javap -p 出力から比較用のメンバ行を取り出して整形・ソートする
  sed 's/^ *//' | grep -v '^Compiled from\|^}$\|^$' | sort
}

cmd_apidiff() {
  local pattern="${1:?Usage: jar-search.sh apidiff <Class> <old-version> [new-version]}"
  local old_version="${2:?Usage: jar-search.sh apidiff <Class> <old-version> [new-version]}"
  local new_version="${3:-$MC_VERSION}"

  local resolved_old jar_old entry_old
  resolved_old=$(resolve_class_entry "$pattern" "$old_version") || return 1
  jar_old="${resolved_old%%:*}"
  entry_old="${resolved_old#*:}"

  local resolved_new jar_new entry_new
  resolved_new=$(resolve_class_entry "$pattern" "$new_version") || return 1
  jar_new="${resolved_new%%:*}"
  entry_new="${resolved_new#*:}"

  echo "=== API diff: $entry_old ==="
  echo "    old ($old_version): ${jar_old#$PROJECT_ROOT/}"
  echo "    new ($new_version): ${jar_new#$PROJECT_ROOT/}"
  echo "    (シグネチャはソート済み。マッピング名の変更も diff に出る点に注意)"
  echo ""

  local sig_old sig_new
  sig_old=$(javap_class "-p" "$jar_old" "$entry_old" | signature_lines) || return 1
  sig_new=$(javap_class "-p" "$jar_new" "$entry_new" | signature_lines) || return 1

  if diff -u --label "MC $old_version" --label "MC $new_version" \
      <(echo "$sig_old") <(echo "$sig_new"); then
    echo "(シグネチャに差分なし)"
  fi
}

cmd_callers() {
  local pattern="" method="" dir_filter=""
  while [ $# -gt 0 ]; do
    case "$1" in
      --dir) dir_filter="$2"; shift 2 ;;
      --all) SKIP_VERSION_FILTER=true; shift ;;
      *)
        if [ -z "$pattern" ]; then pattern="$1"; else method="$1"; fi
        shift ;;
    esac
  done
  if [ -z "$pattern" ]; then
    echo "Usage: jar-search.sh callers <Class> [method] [--dir <subdir>] [--all]" >&2
    return 1
  fi

  # 対象クラスを解決し、定数プール検索用の内部名 (net/minecraft/... 形式) を得る
  local resolved entry internal
  resolved=$(resolve_class_entry "$pattern") || return 1
  entry="${resolved#*:}"
  internal="${entry%.class}"

  local javap_bin=""
  if [ -n "$method" ]; then
    javap_bin=$(resolve_javap) || return 1
  fi

  echo "=== Callers of ${internal}${method:+#$method} ($(version_label)) ==="

  local tmp
  tmp=$(mktemp -d)
  local found=0
  local jar
  while IFS= read -r jar; do
    [ -z "$jar" ] && continue
    case "$jar" in
      *"$dir_filter"*) ;; # --dir 指定時のフィルタ (未指定なら全 jar が通る)
      *) [ -n "$dir_filter" ] && continue ;;
    esac
    # 段階1: jar 全体を stream grep して参照の有無を判定 (展開せず高速に)
    # unzip の SIGPIPE (grep -q の早期終了) を || true で吸収する
    if ! { unzip -p "$jar" '*.class' 2>/dev/null || true; } | grep -qF "$internal"; then
      continue
    fi
    # 段階2: 参照を含む jar のみ展開し、参照元クラスを特定
    local jdir="$tmp/$(basename "$jar" .jar)"
    mkdir -p "$jdir"
    unzip -o -q "$jar" '*.class' -d "$jdir" 2>/dev/null || true
    local hits
    hits=$(grep -rlF "$internal" "$jdir" 2>/dev/null | sort || true)
    [ -z "$hits" ] && continue

    local jar_header_shown=false
    local n_candidates
    n_candidates=$(echo "$hits" | grep -c . || true)
    local skip_javap=false
    if [ -n "$method" ] && [ "$n_candidates" -gt 200 ]; then
      skip_javap=true
    fi

    local f
    while IFS= read -r f; do
      [ -z "$f" ] && continue
      local rel="${f#$jdir/}"
      local base="${rel%.class}"
      # 自己参照 (対象クラス自身とそのインナークラス) を除外
      case "$base" in
        "$internal"|"$internal"\$*) continue ;;
      esac
      local fqcn
      fqcn=$(echo "$base" | tr '/' '.')

      if [ -n "$method" ] && [ "$skip_javap" = false ]; then
        # 候補クラスを javap -c し、実際に対象メソッドを呼んでいるかを確認する。
        # javap の呼び出しコメントは "Method <internal>.<method>:..." 形式
        # ("InterfaceMethod" も "Method" を含むため grep 1 本で両方拾える)
        local calls
        calls=$("$javap_bin" -p -c -classpath "$jdir" "$fqcn" 2>/dev/null \
          | awk -v pat="Method ${internal}.${method}" '
              BEGIN { RS=""; FS="\n" }
              index($0, pat) {
                print "    " $1
                for (i = 2; i <= NF; i++) if (index($i, pat)) print "      " $i
              }' || true)
        [ -z "$calls" ] && continue
        if [ "$jar_header_shown" = false ]; then
          echo ""
          echo "--- ${jar#$PROJECT_ROOT/} ---"
          jar_header_shown=true
        fi
        echo "  $fqcn"
        echo "$calls"
      else
        if [ "$jar_header_shown" = false ]; then
          echo ""
          echo "--- ${jar#$PROJECT_ROOT/} ---"
          jar_header_shown=true
          if [ "$skip_javap" = true ]; then
            echo "  (候補 $n_candidates クラス > 200 のため method 検証を省略し、クラス参照の一覧のみ表示。--dir で絞り込み推奨)"
          fi
        fi
        echo "  $fqcn"
      fi
      found=1
    done <<< "$hits"
  done < <(find_class_jars "")
  rm -rf "$tmp"

  if [ "$found" -eq 0 ]; then
    echo "(no callers found)"
  fi
}

cmd_doctor() {
  echo "=== jar-search.sh doctor ==="
  echo "Project root : $PROJECT_ROOT"
  echo "MC version   : $MC_VERSION"
  echo ""

  if [ ! -d "$LOOM_CACHE" ]; then
    echo "NG: loom-cache が存在しない ($LOOM_CACHE)"
    echo "    → './gradlew build' 等で依存解決を先に実行すること"
    return 0
  fi

  local class_jar_count source_jar_count
  class_jar_count=$(find_class_jars "" | grep -c . || true)
  source_jar_count=$(find_source_jars 2>/dev/null | grep -c . || true)

  # minecraftMaven のソース jar が実際にバニラクラスを含むか確認する。
  # NeoForge パッチのみの sources jar（net/neoforged/ だけ）が存在するため、
  # jar の有無だけではバニラソースの有無を判定できない。
  local mc_source_count=0
  local jar
  while IFS= read -r jar; do
    [ -z "$jar" ] && continue
    if unzip -Z1 "$jar" 2>/dev/null | grep -q "^net/minecraft/"; then
      mc_source_count=$((mc_source_count + 1))
    fi
  done < <(find_source_jars 2>/dev/null | grep "minecraftMaven" || true)

  echo "コンパイル済み jar (現バージョン): $class_jar_count 個"
  echo "ソース jar (現バージョン)        : $source_jar_count 個 (うちバニラクラスを含む Minecraft 本体: $mc_source_count)"
  echo ""

  if [ "$mc_source_count" -eq 0 ]; then
    echo "NG: Minecraft 本体のソース jar が無い（genSources 未実行）"
    echo "    → find/grep/read はバニラクラスにヒットしない"
    echo "    → 対処1: sig / bytecode / apidiff を使う（ソース不要、即時）"
    echo "    → 対処2: './gradlew genSources' でソース生成（数分かかるが read が使える）"
  else
    echo "OK: Minecraft 本体のソース jar あり（find/grep/read が使える）"
  fi
  echo ""

  local javap_bin
  if javap_bin=$(resolve_javap 2>/dev/null); then
    echo "OK: javap = $javap_bin"
  else
    echo "NG: javap が見つからない → sig / bytecode / apidiff は使えない"
    echo "    → JDK を PATH に通すか ~/.gradle/gradle.properties の org.gradle.java.home を設定"
  fi
}

case "${1:-help}" in
  find)     shift; cmd_find "$@" ;;
  grep)     shift; cmd_grep "$@" ;;
  read)     shift; cmd_read "$@" ;;
  list)     shift; cmd_list "${@}" ;;
  sig)      shift; cmd_sig "$@" ;;
  bytecode) shift; cmd_bytecode "$@" ;;
  apidiff)  shift; cmd_apidiff "$@" ;;
  callers)  shift; cmd_callers "$@" ;;
  doctor)   shift; cmd_doctor ;;
  *)
    cat <<'USAGE'
Usage:
  jar-search.sh find <pattern> [--dir <subdir>] [--all]
  jar-search.sh grep <jar-pattern> <pattern> [N]
  jar-search.sh read <jar-pattern> <class-path>
  jar-search.sh list [filter] [--all]
  jar-search.sh sig <Class> [--all]
  jar-search.sh bytecode <Class> [method-pattern] [--all]
  jar-search.sh apidiff <Class> <old-version> [new-version]
  jar-search.sh callers <Class> [method] [--dir <subdir>] [--all]
  jar-search.sh doctor

Global options:
  --all   Disable version filter (include all MC versions in loom-cache)

Version filtering:
  By default, filters jars by minecraft_version from gradle.properties.
  Use --all if jar naming conventions change or to debug version issues.

Source jars vs compiled jars:
  find/grep/read need source jars (genSources). sig/bytecode/apidiff work on
  compiled jars via javap — use them when source jars are missing.
  Run 'doctor' to check which are available.

Examples:
  jar-search.sh find FakePlayer                   # Search all jars for current MC version
  jar-search.sh find TestContext --dir minecraftMaven  # Search Minecraft jars only
  jar-search.sh find SomeClass --all              # Search all versions
  jar-search.sh grep fabric-events-interaction "class FakePlayer"
  jar-search.sh grep minecraft-merged-@common "getWorld" 10
  jar-search.sh read fabric-events-interaction net/fabricmc/fabric/api/entity/FakePlayer.java
  jar-search.sh list fabric                       # List Fabric-related jars
  jar-search.sh list --all                        # List all jars (no version filter)
  jar-search.sh sig Sound                         # Member signatures (javap -p)
  jar-search.sh sig net.minecraft.client.sound.Sound
  jar-search.sh bytecode Sound getLocation        # Bytecode of one member (javap -p -c)
  jar-search.sh bytecode Sound "static"           # Static initializer (constant values)
  jar-search.sh apidiff Sound 1.20.1              # Signature diff old -> current version
  jar-search.sh callers Sound                     # Classes referencing Sound
  jar-search.sh callers Sound getLocation         # Call sites of Sound.getLocation
  jar-search.sh doctor                            # Check source jars / javap availability
USAGE
    ;;
esac
