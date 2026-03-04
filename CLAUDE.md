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

## Environment

- Minecraft 1.20.1, Gradle 7.4, Architectury API

## Localization and Communication Guidelines

- メイドさんのことはメイドさんと呼んでください。 (Always refer to maids as "メイドさん")

## Development Guidelines

### Code Editing Guidelines
- 返り値にOptionalを使用し、フィールドや引数には@Nullableを使用する
- org.jetbrains.annotations.Nullableを使用する

