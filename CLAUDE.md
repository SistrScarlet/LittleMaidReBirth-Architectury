## Project Overview

Little Maid Rebirth (LMRB) is a Minecraft mod that adds tameable maid entities with AI behaviors, implemented using Architectury API for cross-platform compatibility between Fabric and Forge.

## Architecture

- Architectury Loom multi-module: `common/`, `fabric/`, `forge/`
- 共通コードは `common/src/main/java/net/sistr/littlemaidrebirth/`
- エンティティ: `entity/LittleMaidEntity.java` が中心
- AI行動: `entity/goal/` (Goal系), `entity/mode/` (Mode系)
- ターゲティング: `entity/targeting/`
- クライアント: `client/` (renderer, screen, key)
- Mixin: `mixin/`
- モード登録: `api/mode/Modes.java` でModeType定義・ItemMatcher登録・init()で一括登録
- モード判定: ItemMatcherのPriority降順で最初にマッチしたモードが採用される（同Priority時は登録順）
- lang: `assets/littlemaidrebirth/lang/{en_us,ja_jp,zh_cn}.json` — モード名キーは `mode.littlemaidrebirth.{Name}`
- タグ: `data/littlemaidrebirth/tags/items/` — モード用タグは `{mode_name}_mode.json`

## Environment

- Minecraft 1.20.1, Gradle 7.4, Architectury API
- Java 17 が必要（`~/.gradle/gradle.properties` で `org.gradle.java.home` 設定済み）

## Build & Test

- `./gradlew spotlessApply` - コード整形 (google-java-format)
- `./gradlew spotlessCheck` - 整形チェック
- `./gradlew checkstyleMain` - Checkstyle スタイルチェック
- `./gradlew spotbugsMain` - SpotBugs バグ検出
- `python3 .claude/scripts/spotbugs-report.py` - SpotBugs レポート解析（`--summary`, `--priority N`）
- `./gradlew :common:test` - ユニットテスト (JUnit 5)

## Localization and Communication Guidelines

- メイドさんのことはメイドさんと呼んでください。 (Always refer to maids as "メイドさん")

## TODO 管理

- `TODO.md` をタスクリストとして自律管理する
- 開発中に発見した課題・技術的負債・リファクタ候補などを随時追記する
- 完了したタスクは削除する（履歴は不要）
- 優先度（高/中/低）でカテゴリ分けする
- Notion タスクボードは公開されているため、ユーザーの明示的な指示がない限り変更しない（読み取りは自由）

## 作業記録

- 設計判断や重要な技術的決定を行った際は `docs/` に作業記録を残す
- ユーザーの指示がなくても、記録に値する判断をした場合は自律的に `/doc` スキルで記録する
- 形式: `docs/{category}/yyyy-mm-dd_{タイトル}.md`
- カテゴリ: `adr/`（設計判断）, `plan/`（作業プラン）, `research/`（調査メモ）等

## Development Guidelines

## Cross-Environment Workflow

- WSL2 から Windows リポジトリへローカルremote経由で転送可能
- `git remote add local /mnt/v/Develop/Minecraft/LMRB`
- Windows側でチェックアウト中のブランチにはpush不可。別ブランチ名にpush: `git push local 1.20.1:wsl/{branch-name}`

### Code Editing Guidelines
- 返り値にOptionalを使用し、フィールドや引数には@Nullableを使用する
- org.jetbrains.annotations.Nullableを使用する
- @Nullable フィールドはローカル変数にキャッシュしてから使用する（SpotBugs NP_NULL_PARAM_DEREF 対策）
- Mixin Accessor は `util/` に配置し、メソッド名に `_LM` サフィックスを付ける（例: `getBrewTime_LM()`）

