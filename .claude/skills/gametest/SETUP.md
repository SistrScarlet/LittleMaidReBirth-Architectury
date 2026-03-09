# GameTest セットアップガイド

Architectury Loom プロジェクトに GameTest Framework を導入する手順。

## 前提条件

- Minecraft 1.17+
- Architectury Loom 1.1+
- Fabric API（gametest モジュール含む）

## 1. Gradle 設定

### fabric/build.gradle

`loom { runs { } }` ブロックに GameTestServer を追加:

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

**注意**: Architectury Loom 1.1 では `fabricApi { configureTests { ... } }` DSL は使用不可。`loom.runs` で手動定義する。

### forge/build.gradle

`loom { runs { } }` ブロックに追加:

```gradle
loom {
    runs {
        gameTestServer {
            server()
            name "Game Test Server"
            property "forge.enabledGameTestNamespaces", "<modid>"
            runDir "build/gametest"
        }
    }
}
```

`<modid>` はプロジェクトの mod ID に置き換える。

## 2. エントリポイント登録

### Fabric: fabric.mod.json

`entrypoints` に `fabric-gametest` を追加:

```json
{
  "entrypoints": {
    "fabric-gametest": [
      "com.example.mymod.fabric.gametest.MyModGameTests"
    ]
  }
}
```

### Forge

`@GameTestHolder` アノテーションで自動登録されるため、追加設定は不要。

## 3. テストクラスの作成（Architectury パターン）

Architectury プロジェクトではテストロジックを common に集約し、Fabric/Forge から委譲する:

```
common/src/main/java/<package>/gametest/
├── MyModCommonTests.java     ← テストロジック（static メソッド）
└── GameTestHelper.java       ← @ExpectPlatform ヘルパー（任意）

fabric/src/main/java/<package>/fabric/gametest/
└── MyModGameTests.java       ← FabricGameTest 実装、common に委譲

forge/src/main/java/<package>/forge/gametest/
└── MyModForgeGameTests.java  ← @GameTestHolder、common に委譲
```

### Common テストクラス

```java
public final class MyModCommonTests {
    private MyModCommonTests() {}

    public static void myTest(TestContext context) {
        // テストロジック
        context.complete();
    }
}
```

### Fabric テストクラス

```java
public class MyModGameTests implements FabricGameTest {
    private static final String SMALL_FLOOR = "<modid>:small_floor";

    @GameTest(templateName = SMALL_FLOOR)
    public void myTest(TestContext context) {
        MyModCommonTests.myTest(context);  // common に委譲
    }
}
```

### Forge テストクラス

```java
@GameTestHolder("<modid>")
@PrefixGameTestTemplate(false)
public class MyModForgeGameTests {
    private static final String SMALL_FLOOR = "small_floor";  // namespace 不要

    @GameTest(templateName = SMALL_FLOOR)
    public static void myTest(TestContext context) {  // static 必須
        MyModCommonTests.myTest(context);
    }
}
```

## 4. ストラクチャーの準備

最低限、床付きストラクチャーを1つ作成する:

```bash
S=.claude/skills/gametest/scripts/nbt.py

# 8x4x8 の石床ストラクチャーを作成
python3 $S create small_floor.nbt 8 4 8
python3 $S fill small_floor.nbt 0 0 0 7 0 7 stone
```

作成した `.nbt` ファイルを3箇所に配置:

```
common/src/main/resources/data/<modid>/structures/small_floor.nbt
fabric/src/main/resources/data/<modid>/structures/small_floor.nbt
forge/src/main/resources/data/<modid>/structures/small_floor.nbt
```

## 5. 初回実行

### eula.txt

GameTestServer 初回実行時は `eula.txt` が必要。`<platform>/build/gametest/eula.txt` に `eula=true` を書く:

```bash
mkdir -p fabric/build/gametest && echo "eula=true" > fabric/build/gametest/eula.txt
mkdir -p forge/build/gametest && echo "eula=true" > forge/build/gametest/eula.txt
```

### 実行

```bash
# Fabric
./gradlew :fabric:runGameTestServer

# Forge（テスト完了後にサーバーが停止しない既知問題あり）
./gradlew :forge:runGameTestServer

# クライアントから手動実行
./gradlew :fabric:runClient
# ゲーム内で: /test runall
```

### ディレクトリ構成

GameTestServer は通常のクライアントとは別ディレクトリで動作する:

```
fabric/build/gametest/    ← GameTestServer のワールド・ログ
fabric/build/gametest/logs/latest.log  ← テスト失敗の詳細
fabric/run/               ← 通常クライアント
```

## FakePlayer ヘルパー（任意）

テストでプレイヤーが必要な場合、Architectury の `@ExpectPlatform` で FakePlayer を作成するヘルパーを用意すると便利:

### common

```java
public class GameTestHelper {
    @ExpectPlatform
    public static ServerPlayerEntity createFakePlayer(ServerWorld world, String name) {
        throw new AssertionError();
    }
}
```

### fabric

```java
public class GameTestHelperImpl {
    public static ServerPlayerEntity createFakePlayer(ServerWorld world, String name) {
        var profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name);
        return new FakePlayer(world, profile);
    }
}
```

Fabric の `FakePlayer` は `net.fabricmc.fabric.api.entity.FakePlayer`。
Forge は `net.minecraftforge.common.util.FakePlayerFactory` を使用する。
