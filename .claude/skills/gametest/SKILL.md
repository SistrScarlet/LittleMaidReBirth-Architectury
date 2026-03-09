---
name: gametest
description: |
  Minecraft mod の GameTest（ゲーム内統合テスト）を作成するスキル。
  /gametest で呼び出す。「GameTest を書いて」「テストケースを追加して」
  「メイドさんのスポーンテストを作って」「Forge 用のテストも作って」
  などと言われた場合にこのスキルを使用する。
  テスト用のストラクチャー（NBT）生成、Fabric/Forge 両対応のテストクラス作成、
  既存テストへのメソッド追加を行う。
---

# Minecraft GameTest 作成スキル

## 概要

Minecraft 1.17+ に組み込まれた GameTest Framework を使って、mod の機能をゲーム内で自動テストするためのテストケースを作成する。Fabric と Forge で API が異なるため、両方に対応する。

## プロジェクト固有情報

このスキルと同じディレクトリにある `PROJECT.md` にプロジェクト固有の情報がある。
テスト作成時は **必ず PROJECT.md を読んでから作業を開始**すること。

PROJECT.md には以下が記載される:
- modid
- 既存ストラクチャー一覧（名前、サイズ、用途、templateName）
- テストクラスのパスと委譲パターン
- プロジェクト固有のヘルパークラス・メソッド
- 既知の注意事項

テスト作成中に新たなプロジェクト固有の知見を得た場合は、PROJECT.md に追記・更新すること。

## セットアップ

GameTest の Gradle 設定・エントリポイント登録・初回実行手順は `SETUP.md` を参照。
プロジェクトに GameTest を新規導入する場合は、まず SETUP.md の手順に従うこと。

## テストクラスの書き方

### Fabric

```java
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public class MyModGameTests implements FabricGameTest {

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void myTest(TestContext context) {
    // テストロジック
    context.complete();
  }
}
```

- `FabricGameTest` インターフェースを実装する
- メソッドは **instance メソッド**（static ではない）
- `EMPTY_STRUCTURE` は `FabricGameTest` の定数（8x8x8 空気ブロック）
- `fabric.mod.json` の `fabric-gametest` エントリポイントに登録が必要

### Forge

```java
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("modid")
@PrefixGameTestTemplate(false)
public class MyModForgeGameTests {

  @GameTest(templateName = "modid:empty")
  public static void myTest(TestContext context) {
    // テストロジック
    context.complete();
  }
}
```

- `@GameTestHolder` でテスト namespace を指定する
- メソッドは **static** でなければならない（Fabric と異なる）
- `@PrefixGameTestTemplate(false)` を付けないとクラス名がテンプレート名にプレフィックスされる
- `templateName` に namespace を含めない（`@GameTestHolder` が自動付与する）— 例: `"empty"` であって `"modid:empty"` ではない
- `build.gradle` の `loom.runs` に `property "forge.enabledGameTestNamespaces", "modid"` が必要

## Yarn マッピング（1.20.x）

| 用途 | Yarn クラス名 |
|------|-------------|
| テストアノテーション | `net.minecraft.test.GameTest` |
| テストヘルパー | `net.minecraft.test.TestContext` |
| Fabric インターフェース | `net.fabricmc.fabric.api.gametest.v1.FabricGameTest` |
| Forge ホルダー | `net.minecraftforge.gametest.GameTestHolder` |
| Forge プレフィックス制御 | `net.minecraftforge.gametest.PrefixGameTestTemplate` |

## ストラクチャーファイル

### 配置先

ストラクチャーは **common / fabric / forge の3箇所に同一ファイルを配置**する必要がある:

```
common/src/main/resources/data/<modid>/structures/<name>.nbt
fabric/src/main/resources/data/<modid>/structures/<name>.nbt
forge/src/main/resources/data/<modid>/structures/<name>.nbt
```

`templateName` は `"<modid>:<name>"` (Fabric) または `"<name>"` (Forge with @GameTestHolder) で参照する。

### 新規ストラクチャーの作成

nbt.py スクリプト（`.claude/skills/gametest/scripts/nbt.py`）で生成:

```bash
S=.claude/skills/gametest/scripts/nbt.py
# 空間作成（8x4x8）
python3 $S create data/modid/structures/my_test.nbt 8 4 8
# 床を石で敷く
python3 $S fill data/modid/structures/my_test.nbt 0 0 0 7 0 7 stone
# ブロック設置
python3 $S set data/modid/structures/my_test.nbt 3 1 3 furnace facing=north
# 確認
python3 $S info data/modid/structures/my_test.nbt
```

**重要**: nbt.py は整数値を常に TAG_INT で書き込む。Forge は `.nbt`（GZip 圧縮バイナリ）のみ読み込む。

**重要**: `EMPTY_STRUCTURE`（床なし）は使わない。エンティティが落下して `clearArea()` の範囲外に残り、次回テスト起動時に UUID 重複警告が出る。床付きストラクチャーを使うこと。

## TestContext の主要メソッド（Yarn マッピング）

```java
// エンティティ
context.spawnEntity(entityType, relativePos);
context.assertEntityPresent(entityType, relativePos);

// ブロック
context.setBlockState(relativePos, blockState);
context.assertBlockPresent(block, relativePos);
context.assertContainerContains(relativePos, item);

// 汎用
context.assertTrue(condition, "エラーメッセージ");
context.assertFalse(condition, "エラーメッセージ");

// 完了
context.complete();                          // 即座に成功
context.addInstantFinalTask(() -> { ... });  // 毎 tick チェック、例外なしで成功
context.addFinalTaskWithDuration(dur, () -> {}); // duration tick 間チェック
context.waitAndRun(ticks, () -> {});         // N tick 後に1回実行
context.runAtTick(tick, () -> {});           // 指定 tick で実行
```

**注意**: 他のドキュメントで `succeedWhen` / `runAfterDelay` と記載されている場合、Yarn では `addInstantFinalTask` / `waitAndRun` に対応する。

## @GameTest アノテーションのパラメータ

Fabric と Forge で `@GameTest` の定義が異なる。

### 共通パラメータ

| パラメータ | デフォルト | 説明 |
|-----------|-----------|------|
| `templateName` | `""` | ストラクチャーテンプレート名 |
| `tickLimit` | 100 | タイムアウト（tick） |
| `required` | true | false なら失敗してもバッチは継続 |
| `batchId` | `"defaultBatch"` | テストバッチのグループ ID |
| `rotation` | 0 | ストラクチャーの回転 |
| `maxAttempts` | 1 | 最大試行回数 |
| `requiredSuccesses` | 1 | 成功と判定する必要回数 |
| `duration` | 0 | テスト持続時間 |

### Forge 固有パラメータ

| パラメータ | デフォルト | 説明 |
|-----------|-----------|------|
| `templateNamespace` | `""` | テンプレートの namespace（`@GameTestHolder` と併用時は不要） |

## 実行方法

| 方法 | コマンド | 用途 |
|------|---------|------|
| Fabric クライアント | `runClient` → `/test runall` | 手動テスト |
| Fabric GameTestServer | `runGameTestServer` | CI 向け自動実行 |
| Forge クライアント | `runClient` → `/test runall` | 手動テスト |

## FakePlayer

テストでプレイヤーが必要な場合は FakePlayer を使用する。

### 基本（ワールド登録なし）

`interactMob()` 等でプレイヤーを直接渡す場合はワールド登録不要:

```java
// FakePlayer の作成方法はプロジェクトにより異なる。PROJECT.md を参照。
var player = createFakePlayer(context.getWorld(), "test-player");
```

### ワールド登録あり

ワールドからプレイヤーを検索する処理（例: `getTameOwner()`）が必要な場合は、
ワールドの players リストに登録する必要がある。

- 登録: `ServerWorld.onPlayerConnected(player)`
- 削除: `Entity.remove(RemovalReason.DISCARDED)`
- **クリーンアップは必須**: GameTest の `clearArea()` は PlayerEntity を除外するため、FakePlayer は明示的に削除しないとワールドに残り続ける

### 非同期テストでのクリーンアップ

`addInstantFinalTask` 内で検証とクリーンアップを行う:

```java
context.addInstantFinalTask(() -> {
    // 検証
    context.assertTrue(condition, "msg");
    // クリーンアップ（ワールド登録した場合は必須）
    removeFakePlayer(player);
});
```

## テストのライフサイクル

1. テスト開始前: `clearArea()` が実行され、ストラクチャー範囲内の **非 PlayerEntity** を削除、ブロックをリセット
2. ストラクチャー配置
3. テストロジック実行
4. テスト終了: 成功/失敗に関わらずストラクチャー範囲のクリーンアップ

**注意**: ワールドは `runGameTestServer` でも `/test` でも永続化される（毎回フレッシュではない）。前回テストのエンティティが保存されている可能性がある。

## 既知の注意事項

- AI依存の flaky テスト: `@GameTest(maxAttempts = 3)` を指定
- Forge の `runGameTestServer` はテスト完了後にサーバーが停止しない問題がある（1.20.1 確認）
- テスト名は小文字に正規化される
- Forge は `.nbt`（GZip 圧縮バイナリ）のみ読み込む。`.snbt`（テキスト）は読み込まれない
