---
name: release
description: |
  Minecraft mod のリリース作業を実行するスキル。
  /release で呼び出す。ユーザーが「リリースして」「バージョンアップして公開して」
  「GitHub Releases に上げて」などと言った場合にこのスキルを使用する。
  ビルド、タグ作成、リリースノート作成、GitHub Releases への公開を一括で行う。
---

# Release スキル

Minecraft mod (LMRB) のリリース作業を実行する。

## 前提条件

- `gradle.properties` の `mod_version` が更新済みであること
- ワーキングツリーがクリーンであること（未コミットの変更がないこと）
- `gh` CLI が認証済みであること

## 手順

### 1. バージョン確認

`gradle.properties` から `mod_version` を読み取り、ユーザーに確認する。

```
現在のバージョン: X.Y.Z
このバージョンでリリースしますか？
```

バージョンが未更新の場合は、ユーザーに更新の種類（x/y/z）を確認してから `gradle.properties` を編集し、コミットする。

### 2. ビルド

```bash
./gradlew build
```

ビルドが失敗した場合は中断する。

### 3. リリースノート作成

前回のリリースタグからの変更点を確認し、リリースノートをドラフトする。

```bash
# 前回タグの特定
git describe --tags --abbrev=0
# 変更履歴の確認
git log --oneline <前回タグ>..HEAD -- '*.java' '*.json' 'gradle.properties'
```

以下の形式（[Keep a Changelog](https://keepachangelog.com/) ベース）でリリースノートを作成する。コミットの type（feat/fix/refactor 等）から自動分類する。

```markdown
## v{version} (MC {mc_version})

### 追加
- 新機能の説明（feat コミットから）

### 変更
- 既存機能の変更点（refactor, perf コミットから）

### 修正
- バグ修正の説明（fix コミットから）
```

**形式ルール:**
- 各項目はユーザー視点で簡潔に書く（実装詳細ではなく挙動の変化を記述）
- 該当する変更がないカテゴリは省略する
- 内部リファクタ等のユーザーに影響しない変更は記載しない。ただし API 変更など開発者に影響するものは「変更」に含める
- 日本語で記述する

ドラフトをユーザーに提示し、確認を取る。

### 4. タグ作成 & push

```bash
git tag v{version}
git push origin v{version}
```

### 5. GitHub Release 作成

```bash
gh release create v{version} \
  --title "v{version}" \
  -n "{リリースノート}" \
  fabric/build/libs/LMRB-{mc_version}-{version}-Fabric.jar \
  forge/build/libs/LMRB-{mc_version}-{version}-Forge.jar
```

### 6. 完了報告

GitHub Release URL をユーザーに伝える。

## 注意事項

- 各ステップで確認を取りながら進める（特にリリースノートとタグpush）
- JARファイル名は `LMRB-{mc_version}-{version}-Fabric.jar` / `LMRB-{mc_version}-{version}-Forge.jar`
- Minecraft バージョンが変わった場合はJARパスのプレフィックスも変わる点に注意
- `.claude/scripts/` にカスタムアップロードスクリプトがある場合、リリース後にユーザーに実行を提案する
