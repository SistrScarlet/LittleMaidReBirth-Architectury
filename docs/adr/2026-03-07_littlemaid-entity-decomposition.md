# LittleMaidEntity の分割リファクタ

## ステータス

承認済み

## コンテキスト

LittleMaidEntity.java が 2364行に達し、Checkstyle FileLength 違反が発生していた。
メイドさんの全責務（AI Goal登録、安全移動、戦闘、インタラクション、復活演出、NBT読み書き、マルチモデル委譲など）が1クラスに集中しており、SRP に違反していた。

既存の委譲パターン（LMHasInventory, HasModeImpl, MultiModelCompound 等）は機能しているが、
委譲されずに直接ロジックが書かれている箇所が ~1000行存在していた。

## 決定

以下の5クラスを LittleMaidEntity から抽出した：

| クラス | 責務 | 方式 | 削減行数 |
|--------|------|------|---------|
| `MaidSoul` | メイドさんの魂データ | 内部クラス → トップレベル化 | ~40行 |
| `MaidResurrection` | 復活ロジック + パーティクル演出 | static ユーティリティ | ~140行 |
| `LMSafeMovement` | 崖・危険ブロック回避移動 | 委譲クラス (Supplier で DI) | ~180行 |
| `LMGoalInitializer` | AI Goal 登録 | パッケージプライベート static ファクトリ | ~225行 |
| `LMInteractionHandler` | プレイヤー操作ハンドリング | パッケージプライベート static ハンドラ | ~170行 |

結果: **2364行 → 1623行（-741行、-31%）**

## 根拠

### 抽出対象の選定基準
- **ロジックが直接埋まっている箇所**を優先（委譲の中継メソッドは放置）
- **super 呼び出しがない**メソッド群を優先（super 依存は外部委譲不可）
- **自己完結度が高い**ロジック群を優先（依存が少ないほどクリーンに分離可能）

### 抽出しなかったもの
- **tryAttack, damage**: `super` 呼び出しがあり外部委譲不可能
- **attack/shoot (射撃)**: ~80行で抽出効果が薄い
- **委譲メソッド群** (~100行): Java の言語制約による boilerplate であり、複雑度は増やさない
- **NBT読み書き、Spawn同期**: 全委譲先のデータを集約する役割で、分離するメリットが低い

### 設計判断
- `MaidResurrection`, `LMGoalInitializer`, `LMInteractionHandler` は static メソッドのみのユーティリティクラス。状態を持たないオーケストレーション/ファクトリロジックには手続き型が適切と判断
- `LMSafeMovement` は `MobEntity` + `Supplier` で依存注入する委譲クラス。`computeFallDamage` (protected) へのアクセスは `Supplier<Float>` で解決
- パッケージプライベートアクセスを活用し、`getGoalSelector()`, `getTargetSelector()`, `getFleeEntities()`, `getExperiencePoints()` をパッケージ内限定で公開

## 影響

- Checkstyle FileLength 違反が解消された
- 各責務が独立したファイルに分離され、変更の局所化が向上
- 新規ファイル5つが `entity/` パッケージに追加された
- MaidSoul のトップレベル化により、`LittleMaidEntity.MaidSoul` の参照を持つ全ファイル（~10ファイル）の import が変更された
