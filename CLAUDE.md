# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Little Maid Rebirth (LMRB) is a Minecraft mod that adds tameable maid entities with AI behaviors, implemented using Architectury API for cross-platform compatibility between Fabric and Forge.

## Localization and Communication Guidelines

- メイドさんのことはメイドさんと呼んでください。 (Always refer to maids as "メイドさん")

## Development Guidelines

### Code Editing Guidelines
- **common内の共通コードのみ編集すること**: Only edit shared code within the common directory
- 返り値にOptionalを使用し、フィールドや引数には@Nullableを使用する
- org.jetbrains.annotations.Nullableを使用する

## Implementation Constraints

- Minecraftの詳細な仕様を把握できないため、具体的な実装はユーザーに任せること
- gradlewコマンドは使用禁止