---
name: notion-task
description: |
  Notion タスクボードの管理スキル。タスクの一覧表示・作成・更新・完了操作を行う。
  /notion-task で呼び出す。ユーザーが「Notionのタスクを確認して」「Notionにタスクを登録して」
  「Notionのステータスを更新して」などと言った場合にこのスキルを使用する。
  このタスクボードは一般公開されているため、変更操作（create/update/done）は
  ユーザーの明示的な指示がある場合のみ実行し、実行前に必ず確認を取ること。
---

# Notion タスク管理

## 対象データベース

- **データベース名**: タスク
- **Data Source ID**: `16ca7187-f079-8195-8682-000be63e99dc`
- **プロジェクト（LittleMaid ReBirth）**: `https://www.notion.so/16ca7187f079804c8372f7571b8c4269`

## スキーマ

| プロパティ | 型 | 値 |
|----------|------|-----|
| タスク名 | title | 自由入力 |
| ステータス | status | 未着手, 進行中, 一部完了, 完了, アーカイブ |
| 優先度 | select | 低, 中, 高 |
| タグ | multi_select | feature, problem, check, bug, compat, origin, other |
| 概要 | text | 自由入力 |
| 期限 | date | ISO-8601 |
| プロジェクト | relation | ページ URL |
| 担当者 | person | ユーザー ID |

## サブコマンド

### list — タスク一覧

検索にヒットしたタスクを表形式で表示する。

```
/notion-task list
```

**制約:** Notion MCP の search はセマンティック検索のため、最大10件程度しか返らない。全件の正確な一覧が必要な場合は Notion Web で確認すること。list は「ざっくり確認」用途。

**手順:**
1. `mcp__plugin_Notion_notion__notion-search` で data_source_url を指定して検索
2. 各タスクの詳細を `mcp__plugin_Notion_notion__notion-fetch` で取得
3. ステータス・優先度・タグと共に表形式で出力（完了済みは除外）

### create — タスク作成

新しいタスクを作成する。実行前に内容を表示して確認を取る。

```
/notion-task create "タスク名" --priority 中 --tag feature --summary "概要"
```

**手順:**
1. ユーザーの指示からプロパティを組み立てる
2. 作成内容をプレビュー表示し、ユーザーの確認を待つ
3. 確認後、`mcp__plugin_Notion_notion__notion-create-pages` で作成

**create-pages の呼び出し例:**
```json
{
  "parent": {"data_source_id": "16ca7187-f079-8195-8682-000be63e99dc"},
  "pages": [{
    "properties": {
      "タスク名": "タスク名をここに",
      "ステータス": "未着手",
      "優先度": "中",
      "タグ": "[\"feature\"]",
      "概要": "概要テキスト",
      "プロジェクト": "[\"https://www.notion.so/16ca7187f079804c8372f7571b8c4269\"]"
    }
  }]
}
```

- プロジェクトは指定がなければ LittleMaid ReBirth をデフォルトで設定する
- ステータスは指定がなければ「未着手」をデフォルトにする

### update — タスク更新

既存タスクのプロパティを更新する。実行前に変更内容を表示して確認を取る。

```
/notion-task update "タスク名" --status 進行中
```

**手順:**
1. タスク名で検索し、対象タスクを特定する
2. 現在の値と変更後の値を並べて表示し、確認を待つ
3. 確認後、`mcp__plugin_Notion_notion__notion-update-page` で更新

### done — タスク完了

タスクのステータスを「完了」に変更する。update の簡略版。

```
/notion-task done "タスク名"
```

**手順:**
1. タスク名で検索し、対象タスクを特定する
2. 完了にする旨を表示し、確認を待つ
3. 確認後、ステータスを「完了」に更新

## 重要な注意事項

- このタスクボードは一般公開されている。変更操作は慎重に行うこと。
- 読み取り（list）は自由に行ってよいが、書き込み（create/update/done）はユーザーの明示的な指示と確認の両方が必要。
- 出力は全て日本語で行う。
- メイドさんのことはメイドさんと呼ぶこと。
