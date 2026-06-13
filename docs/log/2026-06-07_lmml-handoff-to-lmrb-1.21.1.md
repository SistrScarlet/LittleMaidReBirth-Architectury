# LMML → LMRB 1.21.1 並行作業ハンドオフ (2026-06-07)

LMML 1.21.1 移植セッションから LMRB 1.21.1 移植セッションへのコンテキスト引き継ぎ。
LMRB セッション開始時に **まずこのファイルを Read してから作業を始めること**。

## 結論: LMRB 1.21.1 並行作業 GO

LMML 9.0.0 の jar (fabric / neoforge 両方) はビルド済み・実機起動確認済み。LMRB 移植プランのフェーズ 1-5 は LMML 残作業と独立して進められる。フェーズ 6 (GameTest) 前に LMML 実機検証完了が直列点。

## LMML 1.21.1 現状 (2026-06-07 時点)

### 動作確認済み
- fabric クライアント起動 OK
- メイドさん (MultiModelEntity) 召喚 OK
- ModelSelectScreen のモデル描画 OK
- 内蔵モデル (Default) でのレンダリング OK
- ネットワーク (CustomPayload) OK
- リソースパック登録 (ResourceWrapper / LMPackProvider) OK
- GLSL shader (lmml_emissive) コンパイル OK

### 未検証 (LMRB のフェーズ 6 前までに片付ける必要あり)
- **外部 model pack (.class) ロード** — MultiModelClassLoader の本番。`ClassWriter#getCommonSuperClass` の NeoForge ModuleLayer + Java 21 問題が顕在化するかも (フェーズ 4 静的解析参照)
- 防具モデル選択 GUI / 防具装着レンダリング
- SoundPackSelectScreen + 音声再生
- LMMLConfig 設定画面
- neoforge runClient 起動

### TODO (LMML 側、LMRB に影響しうるもの)
- **高**: caps 対応が不完全 — 1.21 で LivingEntity API が変更された関係で `caps_roll` 等を placeholder (`(entity, arg) -> 0`) で逃している。メイドさんのアニメーション (riptide / 一部モーション) に影響する可能性あり。LMRB フェーズ 6 前に埋め直す
- 中: 防具モデル選択 GUI の解除機能
- 低: LivingVoiceRate, アイテム保持時の描画ズレ, 記号ファイル名の二重登録, アーマー光沢

## LMRB 並行作業の段取り

| フェーズ | LMML 依存度 | 並行可否 |
|------|-----|-----|
| 1: ビルド環境 | jar 必要 | OK (jar は既にある) |
| 2: バニラ API 追従 | 低 | OK |
| 3: Mixin 再検証 | 低 | OK |
| 4: データパック JSON | なし | OK |
| 5: Enchantment | なし | OK |
| 6: GameTest 移植 | **高** | LMML 実機検証完了が前提 |
| 7: Cloth Config | なし | OK |

### LMML jar の置き場所
- `/home/sistr/works/mc/LittleMaidModelLoader-Architectury/fabric/build/libs/LMML-1.21.1-9.0.0-Fabric.jar`
- `/home/sistr/works/mc/LittleMaidModelLoader-Architectury/neoforge/build/libs/LMML-1.21.1-9.0.0-NeoForge.jar`

LMRB の `mods/` ディレクトリに投入してフェーズ 1 ビルド環境を組む。

## 参照すべき LMML 文書 (絶対パスで Read 可)

- **移植時の learning 集大成 (最重要)**:
  `/home/sistr/works/mc/LittleMaidModelLoader-Architectury/docs/research/2026-06-06_lmml-1.21.1-migration-lessons.md`
  → NeoForge 21.1 入口クラス template, CustomPayload 二重登録回避パターン, fog_distance 新シグネチャ, ResourcePackProfile.create 仕様変更, PackResourceMetadata 3-arg コンストラクタ必須, Platform.isForge() deprecated 扱い、等。**LMRB の NeoForge 移植で同じ罠を踏むので必読**

- **MultiModelClassLoader 静的解析**:
  `/home/sistr/works/mc/LittleMaidModelLoader-Architectury/docs/research/2026-06-06_lmml-1.21.1-phase4-classloader-static-analysis.md`
  → ASM ClassWriter#getCommonSuperClass の Module Layer 問題、protective wrapper 案。LMRB はこのコードを直接呼ばないが、メイドさんがロードできない症状が出たらここを疑う

- **LMML セッションログ (タイムライン全体)**:
  `/home/sistr/works/mc/LittleMaidModelLoader-Architectury/docs/log/2026-06-07_lmml-1.21.1-phase2-7-and-real-launch.md`
  → 全 11 コミットの何をどう直したかの時系列。LMRB で類似クラッシュが出たら参照

- **LMRB 自身の移植計画**:
  `/home/sistr/works/mc/LittleMaidReBirth-Architectury/docs/plan/2026-06-06_lmrb-1.21.1-migration.md`
  → フェーズ 0-9 のロードマップ。フェーズ 0 (LMML 1.21.1 stable) は本ハンドオフをもって完了扱いで OK

## 既知のリスク・注意点

- **LMML API 表面の破壊的変更**: LMML 8.x → 9.0.0 で API が一部変わっている可能性。LMRB の `IHasMultiModel` / `IMultiModel` / `TextureHolder` / `LMModelManager` / `EntityCaps` / `Networking` 参照箇所でコンパイルエラーが出たら個別対応
- **caps 不完全による副作用**: LMML の caps が float→int 等で placeholder 化されている箇所で、LMRB のメイドさん挙動 (アニメ・モード) に微妙な影響が出るかも。実機で違和感を覚えたら LMML 側 TODO の「高優先 caps 対応」を先に潰す
- **mc-api-research agent 不在**: バニラ API 調査エージェントが現状動かないので、`gh search code` / `gh api` / MC client jar の javap で代用 (LMML セッションで実証済み)

## Cross-Environment Push 規約 (再掲)

- WSL → Windows ローカル remote へは、Windows 側で checkout 中のブランチに直接 push できない
- `git push local 1.21.1:wsl/1.21.1` のように `wsl/{branch-name}` で別名 push する

## 環境メタ情報

- 依存バージョンは LMRB 移植計画書の表 (Yarn 1.21.1+build.3 / Architectury 13.0.8 / Architectury Loom 1.7.435 / Fabric Loader 0.16.10+ / Fabric API 0.116.4+1.21.1 / NeoForge 21.1.233 / Cloth Config 15.0.140 / ModMenu 11.0.4) を踏襲
- Java 17 → Java 21
- forge は切る (LMML と同じ方針)

## 次セッション開始時の推奨手順

1. **このファイルを Read**
2. `docs/plan/2026-06-06_lmrb-1.21.1-migration.md` を Read
3. mc 配下の `~/.claude/projects/-home-sistr-works-mc/memory/MEMORY.md` を一度 Read (project key が変わって自動ロードされない可能性があるため)
4. `git branch --show-current` で `1.20.1` から `1.21.1` ブランチを切る (`git checkout -b 1.21.1`)
5. フェーズ 1 (ビルド環境) 着手
