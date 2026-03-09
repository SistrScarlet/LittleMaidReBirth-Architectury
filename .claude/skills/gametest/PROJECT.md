# GameTest プロジェクト固有情報

<!-- このファイルは LMRB 固有の GameTest 情報を管理する。 -->
<!-- テスト作成時に自動的に参照・更新される。 -->

## 基本情報

- **modid**: `littlemaidrebirth`
- **Minecraft バージョン**: 1.20.1
- **nbt.py DATA_VERSION**: 3465

## テストクラス

テストロジックは Common に集約し、Fabric/Forge から委譲するパターン。

| プラットフォーム | クラスパス | 備考 |
|----------------|-----------|------|
| Common | `common/.../gametest/LMRBCommonTests.java` | static メソッド。全テストロジックをここに書く |
| Fabric | `fabric/.../fabric/gametest/LMRBGameTests.java` | instance メソッド。Common に委譲 |
| Forge | `forge/.../forge/gametest/LMRBForgeGameTests.java` | static メソッド。Common に委譲 |

**委譲パターン例:**

```java
// Fabric（instance メソッド）
@GameTest(templateName = SMALL_FLOOR)
public void myTest(TestContext context) {
    LMRBCommonTests.myTest(context);
}

// Forge（static メソッド）
@GameTest(templateName = SMALL_FLOOR)
public static void myTest(TestContext context) {
    LMRBCommonTests.myTest(context);
}
```

新しいテストを追加する場合、3ファイルすべてにメソッドを追加すること。

## 既存ストラクチャー

| ID | サイズ | 用途 | Fabric templateName | Forge templateName |
|----|--------|------|--------------------|--------------------|
| `small_floor` | 8x4x8 石床 | デフォルト（ほとんどのテスト） | `"littlemaidrebirth:small_floor"` | `"small_floor"` |
| `floor` | 21x4x21 石床 | 広い空間が必要（追従・テレポート） | `"littlemaidrebirth:floor"` | `"floor"` |
| `archer_arena` | 8x4x8 石床+ガラス壁+フェンス仕切り | 射撃テスト | `"littlemaidrebirth:archer_arena"` | `"archer_arena"` |
| `empty` | (未使用) | **使わないこと** — 床なしで落下する | - | - |

各テストクラスで `SMALL_FLOOR` / `FLOOR` として定数定義済み。

ストラクチャーは common/fabric/forge の `resources/data/littlemaidrebirth/structures/` に3箇所同一配置。

## ヘルパークラス・メソッド

### GameTestHelper（Architectury @ExpectPlatform）

`common/.../gametest/GameTestHelper.java`

```java
// FakePlayer 作成（プラットフォーム別実装）
GameTestHelper.createFakePlayer(ServerWorld world, String name) → ServerPlayerEntity

// ワールド登録（getTameOwner() 等で検索可能にする）
GameTestHelper.registerPlayerInWorld(ServerWorld world, ServerPlayerEntity player)

// ワールドから削除
GameTestHelper.removePlayerFromWorld(ServerPlayerEntity player)
```

### LMRBCommonTests 内ヘルパー（private）

```java
spawnMaid(context) → LittleMaidEntity           // メイドさんを (1,1,1) にスポーン
createPlayer(context, name) → ServerPlayerEntity // FakePlayer 作成
holdItem(player, stack)                          // プレイヤーの手にアイテム持たせる
spawnTamedMaid(context, owner) → LittleMaidEntity // テイム済みメイドさんをスポーン
createWorldPlayer(context, name) → ServerPlayerEntity // FakePlayer作成+ワールド登録
cleanupWorldPlayers(players...)                  // complete() 前に呼ぶ
```

## 実行コマンド

- Fabric GameTestServer: `./gradlew :fabric:runGameTestServer`
- Fabric クライアント: `./gradlew :fabric:runClient` → `/test runall`
- テストログ: `fabric/build/gametest/logs/latest.log`

## プロジェクト固有の注意事項

- AI動作テスト（モード作業・格納等）: エンティティ・ブロックは y=2 に配置（y=1 は SMALL_FLOOR の床と同じ高さ）
- 死亡→魂生成等、ワールドの tick が必要なテストは `tickLimit = 200` を設定する
- FakePlayer のネットワーク系処理は no-op（パケット送信不可）、`startRiding()` は常に false
- コンフィグ変更テスト: try/finally でフィールドを直接書き換え+復元
- テスト設計・分類: `docs/research/2026-03-07_gametest-feature-classification.md`
