# LittleMaidReBirth-Architectury

Minecraft 1.20.1 向けメイドさん mod（Fabric/Forge 両対応）

## セットアップ

- Java 17
- `~/.gradle/gradle.properties` に以下を追加:

```properties
org.gradle.java.home=/path/to/java-17
```

## ツールのセットアップ

`.claude/hooks` で使用する jar をダウンロード:

```bash
mkdir -p .claude/tools

# google-java-format
curl -L -o .claude/tools/google-java-format-1.17.0-all-deps.jar \
  https://github.com/google/google-java-format/releases/download/v1.17.0/google-java-format-1.17.0-all-deps.jar

# checkstyle
curl -L -o .claude/tools/checkstyle-10.21.4-all.jar \
  https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.21.4/checkstyle-10.21.4-all.jar
```

## ビルド

```bash
./gradlew build
```

成果物: `fabric/build/libs/`, `forge/build/libs/`

## 開発コマンド

| コマンド | 説明 |
|----------|------|
| `./gradlew spotlessApply` | コード整形 (google-java-format) |
| `./gradlew spotlessCheck` | 整形チェック |
| `./gradlew checkstyleMain` | スタイルチェック |
| `./gradlew spotbugsMain` | バグ検出 |
| `./gradlew :common:test` | テスト |

## ライセンス

LittleMaid Licence — [LICENSE](LICENSE) 参照
