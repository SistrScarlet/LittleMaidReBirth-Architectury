# GameTest Framework 導入調査

## 背景

LittleMaidEntity のリファクタリング後、テスト導入の可能性を検討した。
JUnit で単体テスト可能な純粋ロジックがほぼ存在しないため、
Minecraft の GameTest Framework（1.17+ バニラ組み込み）を調査した。

## 調査結果

### セットアップ（Fabric + Architectury Loom 1.1）

- `FabricGameTest` インターフェースを実装したクラスを作成
- `fabric.mod.json` の `fabric-gametest` エントリポイントに登録
- `fabricApi { configureTests { ... } }` DSL は Architectury Loom 1.1 では **使用不可**
- 代わりに `loom { runs { } }` ブロックで GameTestServer を手動定義

```gradle
loom {
    runs {
        gameTestServer {
            server()
            name "Game Test Server"
            vmArg "-Dfabric-api.gametest"
            vmArg "-Dfabric-api.gametest.report-file=${project.buildDir}/junit.xml"
            runDir "build/gametest"
        }
    }
}
```

### テストの書き方

```java
public class LMRBGameTests implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void maidSpawn(TestContext context) {
        var maid = context.spawnEntity(Registration.LITTLE_MAID_MOB.get(), new BlockPos(1, 1, 1));
        context.assertTrue(maid != null, "メイドさんがスポーンできること");
        context.complete();
    }
}
```

- Yarn マッピング: `net.minecraft.test.GameTest`（アノテーション）, `net.minecraft.test.TestContext`（ヘルパー）
- `EMPTY_STRUCTURE`: 8x8x8 の空気ブロック空間。ストラクチャーファイル不要
- ブロック配置が必要なテスト（かまど作業等）はゲーム内 structure block で SNBT を作成

### 実行方法

| 方法 | コマンド/タスク | 用途 | 動作確認 |
|------|--------------|------|---------|
| クライアント内 | `runClient` → `/test runall` | 手動テスト・目視確認 | ✅ |
| GameTestServer | `./gradlew :fabric:runGameTestServer` | CI 向け自動実行 | ✅ |
| 通常サーバー | `runServer` → `/test runall` | 専用サーバーでの手動テスト | 未確認 |

### ディレクトリ分離

- クライアント: `fabric/run/`
- GameTestServer: `fabric/build/gametest/`（`runDir` で指定）

### テスト可能な対象

| テスト内容 | ストラクチャー | 価値 |
|-----------|-------------|------|
| メイドさんスポーン・契約 | EMPTY_STRUCTURE | 高 |
| 手持ちアイテムでモード切替 | EMPTY_STRUCTURE | 高 |
| かまど/醸造台で作業 | 要カスタム | 高 |
| 祭壇で復活 | 要カスタム | 中 |
| 崖手前で停止 | 要カスタム | 中 |
| 給料未払い→ストライキ | EMPTY_STRUCTURE | 中 |

### 注意事項・制約

- Architectury の common モジュールにテストを書いて両プラットフォームで共有する仕組みは不明。現状 Fabric 側にのみテストを配置
- `initGoals` は `MobEntity` コンストラクタから呼ばれるため、外部クラスからフィールド参照する際は遅延評価が必要（今回のリファクタで発見）
- GameTestServer 初回実行時は `eula.txt`（`eula=true`）が必要

## 結論

- GameTest Framework は Architectury Loom 1.1 + Fabric で動作する
- CI 統合も `runGameTestServer` の exit code で可能
- まずは EMPTY_STRUCTURE で書けるテスト（スポーン、契約、モード切替）から拡充するのが現実的
