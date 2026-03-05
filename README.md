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

## リリース配布のセットアップ

### Dropbox API

1. https://www.dropbox.com/developers/apps でアプリを作成（Scoped access, Full Dropbox）
2. Permissions: `files.content.write` を有効化
3. リフレッシュトークンを取得:
   - ブラウザで `https://www.dropbox.com/oauth2/authorize?client_id={APP_KEY}&response_type=code&token_access_type=offline` にアクセス
   - 認可コードを取得し、`curl -X POST https://api.dropboxapi.com/oauth2/token -u "{APP_KEY}:{APP_SECRET}" -d grant_type=authorization_code -d code={CODE}` を実行
4. 設定ファイルを作成:

```bash
mkdir -p ~/.config/lmrb
cat > ~/.config/lmrb/dropbox.json << 'EOF'
{
  "app_key": "...",
  "app_secret": "...",
  "refresh_token": "..."
}
EOF
chmod 600 ~/.config/lmrb/dropbox.json
```

### GitHub CLI

`gh` コマンドで認証済みであること（`gh auth login`）。

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
