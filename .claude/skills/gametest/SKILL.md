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

### 不要な場合

ブロック配置が不要なテスト（エンティティのスポーン・状態確認など）は空のストラクチャーを使う：
- Fabric: `EMPTY_STRUCTURE` 定数を使用（ファイル不要）
- Forge: `data/<modid>/structures/` に空の NBT ファイルを配置

### 必要な場合

ブロック配置が必要なテスト（かまど作業、醸造台操作など）はストラクチャーファイルを作成する。

**nbt.py スクリプトがプロジェクトにある場合**（`.claude/scripts/nbt.py`）、CLI でストラクチャーを生成できる：

```bash
S=.claude/scripts/nbt.py
# 空間作成（8x4x8）
python3 $S create data/modid/structures/cooking_test.nbt 8 4 8
# 床を石で敷く
python3 $S fill data/modid/structures/cooking_test.nbt 0 0 0 7 0 7 stone
# かまど設置
python3 $S set data/modid/structures/cooking_test.nbt 3 1 3 furnace facing=north
# 確認
python3 $S info data/modid/structures/cooking_test.nbt
```

nbt.py がない場合、このスキルの `scripts/nbt.py` をプロジェクトにコピーして使用する。

**重要**: Forge は `.nbt`（GZip 圧縮バイナリ）のみ読み込む。`.snbt`（テキスト）は読み込まれない。

### ストラクチャーの配置先

```
common/src/main/resources/data/<modid>/structures/<name>.nbt
```

`templateName` は `"<modid>:<name>"` (Fabric) または `"<name>"` (Forge with @GameTestHolder) で参照する。

## TestContext の主要メソッド

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
context.complete();                    // 即座に成功
context.succeedWhen(() -> { ... });    // 条件が満たされるまで毎 tick チェック
context.runAtTickTime(tick, () -> {}); // 指定 tick で実行
context.runAfterDelay(delay, () -> {}); // N tick 後に実行
```

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

## 既知の注意事項

- Forge の `runGameTestServer` はテスト完了後にサーバーが停止しない問題がある（1.20.1 確認）
- テスト名は小文字に正規化される
- Forge は `.nbt`（GZip 圧縮バイナリ）のみ読み込む。`.snbt`（テキスト）は読み込まれない
