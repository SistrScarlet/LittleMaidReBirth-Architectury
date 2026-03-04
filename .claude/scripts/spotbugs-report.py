#!/usr/bin/env python3
"""SpotBugs XML レポートを解析して見やすく出力するスクリプト。

Usage:
  python3 .claude/scripts/spotbugs-report.py [options]

Options:
  --priority N    指定した優先度のみ表示 (例: --priority 1)
  --summary       パターン別件数のサマリーのみ表示
  --file PATH     解析対象の XML ファイルパス (デフォルト: common モジュール)
"""

import argparse
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

DEFAULT_REPORT = "common/build/reports/spotbugs/main.xml"


def find_report(specified_path):
    if specified_path:
        return Path(specified_path)
    script_dir = Path(__file__).resolve().parent
    project_root = script_dir.parent.parent
    return project_root / DEFAULT_REPORT


def parse_report(report_path):
    if not report_path.exists():
        print(f"Report not found: {report_path}", file=sys.stderr)
        print("Run: ./gradlew spotbugsMain", file=sys.stderr)
        sys.exit(1)

    tree = ET.parse(report_path)
    root = tree.getroot()

    bugs = []
    for bi in root.iter("BugInstance"):
        bug = {
            "type": bi.get("type"),
            "priority": int(bi.get("priority", "0")),
            "category": bi.get("category", ""),
        }

        for sl in bi.iter("SourceLine"):
            start = sl.get("start", "")
            if start:
                bug["source"] = sl.get("sourcepath", "")
                bug["line"] = start
                break

        for m in bi.iter("Method"):
            bug["method"] = m.get("name", "")
            break

        for f in bi.iter("Field"):
            bug["field"] = f.get("name", "")
            break

        bugs.append(bug)

    return bugs


def print_summary(bugs):
    print("=== Priority Summary ===")
    priority_count = Counter(b["priority"] for b in bugs)
    for p in sorted(priority_count):
        print(f"  P{p}: {priority_count[p]} bugs")
    print()

    print("=== Pattern Summary ===")
    pattern_count = Counter(b["type"] for b in bugs)
    for pattern, count in pattern_count.most_common():
        print(f"  {count:3d} {pattern}")
    print()
    print(f"Total: {len(bugs)} bugs")


def print_details(bugs):
    by_priority = {}
    for b in bugs:
        by_priority.setdefault(b["priority"], []).append(b)

    for p in sorted(by_priority):
        print(f"\n=== P{p} ({len(by_priority[p])} bugs) ===")
        for b in sorted(by_priority[p], key=lambda x: (x.get("source", ""), x.get("line", ""))):
            source = b.get("source", "?")
            line = b.get("line", "?")
            method = b.get("method", "")
            field = b.get("field", "")
            extra = ""
            if method:
                extra += f" method={method}"
            if field:
                extra += f" field={field}"
            print(f"  [{b['type']}] {source}:{line}{extra}")


def main():
    parser = argparse.ArgumentParser(description="SpotBugs report analyzer")
    parser.add_argument("--priority", type=int, help="Filter by priority level")
    parser.add_argument("--summary", action="store_true", help="Show summary only")
    parser.add_argument("--file", type=str, help="Path to SpotBugs XML report")
    args = parser.parse_args()

    report_path = find_report(args.file)
    bugs = parse_report(report_path)

    if args.priority:
        bugs = [b for b in bugs if b["priority"] == args.priority]

    if not bugs:
        print("No bugs found!")
        return

    print_summary(bugs)
    if not args.summary:
        print_details(bugs)


if __name__ == "__main__":
    main()
