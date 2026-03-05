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

前回のリリースタグからの変更点を確認し、ユーザー向けのリリースノートをドラフトする。

```bash
# 前回タグの特定
git describe --tags --abbrev=0
# 変更履歴の確認
git log --oneline <前回タグ>..HEAD -- '*.java' '*.json' 'gradle.properties'
```

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
  fabric/build/libs/LMRB-1.20.1-{version}-Fabric.jar \
  forge/build/libs/LMRB-1.20.1-{version}-Forge.jar
```

### 6. Dropbox アップロード

```bash
python3 .claude/scripts/dropbox-upload.py --version {version} [--mc-version 1.20.1]
```

既存のフォルダ構造 `/Mods/LittleMaidReBirth/{Fabric,Forge}/{mc_folder}/` にアップロードされる。

### 7. 完了報告

GitHub Release URL をユーザーに伝える。

## 注意事項

- 各ステップで確認を取りながら進める（特にリリースノートとタグpush）
- JARファイル名は `LMRB-{mc_version}-{version}-Fabric.jar` / `LMRB-{mc_version}-{version}-Forge.jar`
- Minecraft バージョンが変わった場合はJARパスのプレフィックスも変わる点に注意
- Dropbox の MC バージョンフォルダマッピングは `.claude/scripts/dropbox-upload.py` の `MC_VERSION_FOLDER` で管理
