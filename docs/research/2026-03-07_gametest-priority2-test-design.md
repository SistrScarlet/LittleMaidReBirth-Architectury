# 第2優先グループ テスト設計

## 対象機能

1. 追従（Escort）
2. テレポート
3. Fencer モード
4. ダメージ処理（コンフィグ分岐）
5. 死亡・魂生成
6. NBT 読み書き

## 技術的な課題と解決策

### FakePlayer ワールド登録

Goal 系テスト（追従・テレポート）は `TameableUtil.getTameOwner()` → `world.getPlayerByUuid()` でオーナーを検索するため、FakePlayer がワールドに登録されている必要がある。

- `GameTestHelper.registerPlayerInWorld(world, player)` → `ServerWorld.onPlayerConnected(player)` で登録
- テスト後に `cleanupWorldPlayers(player)` → `Entity.remove(RemovalReason.DISCARDED)` で削除
- `clearArea()` は PlayerEntity を除外するため、明示的な削除が必須

### ストラクチャー

- ESC/TP テストは `FLOOR`（21x4x21 石床）を使用（距離テストに十分な広さ）
- その他のテストは `SMALL_FLOOR`（8x4x8 石床）を使用

---

## 1. 追従 Goal（ESC）

### テスト対象

- `HasMMFollowTameOwnerGoal.canStart()` — ESCORT モード + 距離 + Wait 条件

### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| ESC-1 | 距離が遠いとき追従Goalが開始可能 | テイム済み + ESCORT + プレイヤーを距離9に配置 | Goal インスタンス化 + canStart() | true |
| ESC-2 | 距離が近いとき追従Goalが開始しない | テイム済み + ESCORT + プレイヤーを距離2に配置 | 同上 | false |
| ESC-3 | 待機中は追従Goalが開始しない | テイム済み + Wait=true + 距離9 | 同上 | false |
| ESC-4 | FREEDOMモードでは追従しない | テイム済み + FREEDOM + 距離9 | 同上 | false |

### 補足

- `createWorldPlayer` でプレイヤーを生成し、`refreshPositionAndAngles(context.getAbsolutePos(...))` で位置を設定
- Goal はテスト内で直接インスタンス化してテスト（AI ループに依存しない）

---

## 2. テレポート Goal（TP）

### テスト対象

- `LMTeleportTameOwnerGoal.canStart()` — ESCORT モード + 距離条件
- テレポート実行（`addInstantFinalTask` で AI に任せる）

### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| TP-1 | 距離がteleportStartDistance超で条件成立 | テイム済み + ESCORT + 距離18 | Goal.canStart() | true |
| TP-2 | 距離が近いとテレポートしない | テイム済み + ESCORT + 距離4 | Goal.canStart() | false |
| TP-3 | FREEDOMモードではテレポートしない | テイム済み + FREEDOM + 距離18 | Goal.canStart() | false |
| TP-4 | テレポート実行後にオーナー近くに移動 | テイム済み + ESCORT + 距離24 | addInstantFinalTask で距離確認 | teleportWidth+2 以内 |

### 補足

- TP-4 は AI のテレポート処理を待つため `addInstantFinalTask` + `tickLimit = 200` を使用
- テレポート先はランダムだが、成功すればオーナー近くに移動するはず

---

## 3. Fencer モード（FEN）

### テスト対象

- `HasModeImpl.tick()` でのモード判定
- `FencerMode.shouldExecute()` — ターゲット + 武器チェック
- `LittleMaidEntity.tryAttack()` — ダメージ付与

### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| FEN-1 | 剣を持つとFencerモード | テイム済み + ダイヤ剣装備 | hasModeImpl.tick() | getMode() = "Fencer" |
| FEN-2 | 斧を持つとFencerモード | テイム済み + 鉄斧装備 | hasModeImpl.tick() | getMode() = "Fencer" |
| FEN-3 | 弓ではFencerにならない | テイム済み + 弓装備 | hasModeImpl.tick() | getMode() != "Fencer" |
| FEN-4 | tryAttackでダメージを与える | テイム済み + ダイヤ剣 + ゾンビ | tryAttack(zombie) | ゾンビの HP 減少 |
| FEN-5 | ターゲットありでshouldExecute=true | テイム済み + ダイヤ剣 + setTarget(zombie) | shouldExecute() | true |
| FEN-6 | ターゲットなしでshouldExecute=false | テイム済み + ダイヤ剣 + setTarget(null) | shouldExecute() | false |

---

## 4. ダメージ処理（DMG）

### テスト対象

- `LittleMaidEntity.damage()` のコンフィグ分岐

### テストケース

| ID | テスト名 | コンフィグ | 操作 | 検証項目 |
|----|---------|----------|------|---------|
| DMG-1 | モブからの通常ダメージを受ける | デフォルト | ゾンビが攻撃 | HP 減少 |
| DMG-2 | immortal=trueでダメージ無効 | immortal=true（要復元） | ゾンビが攻撃 | HP 変化なし |
| DMG-3 | fallImmunity=trueで落下ダメージ無効 | fallImmunity=true（要復元） | 落下ダメージ | HP 変化なし |
| DMG-4 | nonMobDamageImmunityでモブ以外無効 | nonMobDamageImmunity=true（要復元） | attacker=null のダメージ | HP 変化なし |
| DMG-5 | 待機中にダメージでWait解除 | テイム済み + Wait | ゾンビが攻撃 | isSitting=false |

---

## 5. 死亡・魂生成（SOUL）

### テスト対象

- `LittleMaidEntity.remove()` → `MaidSoulEntity` スポーン
- `MaidSoul.getOwnerUUID()`

### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| SOUL-1 | テイム済みメイドさんの死亡で魂生成 | テイム済み | kill() | MaidSoulEntity がテスト範囲内に存在 |
| SOUL-2 | 野良メイドさんの死亡では魂なし | 野良 | kill() | MaidSoulEntity が存在しない |
| SOUL-3 | 魂がオーナーUUIDを保持 | テイム済み | kill() | soul.getOwnerUUID() = オーナーUUID |

### 補足

- `kill()` で即死させる（`damage(outOfWorld)` より確実）
- `addInstantFinalTask` で MaidSoulEntity の存在をチェック（`tickLimit = 200`）
- エンティティ検索は `Box` で範囲制限（他テストの魂との干渉回避）
- SOUL-2 は `waitAndRun(40, ...)` で遅延チェック

---

## 6. NBT 読み書き（NBT）

### テスト対象

- `writeNbt()` / `readNbt()` による状態の保存・復元

### テストケース

| ID | テスト名 | 保存する状態 | 検証項目 |
|----|---------|------------|---------|
| NBT-1 | テイム状態 | ownerUuid | isTamed=true, UUID 一致 |
| NBT-2 | 待機状態 | Wait=true | isSitting=true |
| NBT-3 | 移動モード | FREEDOM | getMovingMode()=FREEDOM |
| NBT-4 | ストライキ | Strike=true | isStrike=true |
| NBT-5 | 吸血モード | BloodSuck=true | isBloodSuck=true |
| NBT-6 | インベントリ | メインハンドにダイヤ剣 | MAINHAND=DIAMOND_SWORD |
| NBT-7 | 経験値 | experiencePoints=100 | getXpToDrop()=100 |

### パターン

```java
var nbt = new NbtCompound();
maid.writeNbt(nbt);
var maid2 = spawnMaid(context);
maid2.readNbt(nbt);
// maid2 の状態を検証
```

---

## ケース数

| カテゴリ | ケース数 | コンフィグ変更 | ストラクチャー | 実装状況 |
|---------|---------|-------------|-------------|---------|
| 追従 Goal (ESC) | 4 | なし | FLOOR | 済 |
| テレポート Goal (TP) | 4 | なし | FLOOR | 済 |
| Fencer モード (FEN) | 6 | なし | SMALL_FLOOR | 済 |
| ダメージ処理 (DMG) | 5 | 3ケース | SMALL_FLOOR | 済 |
| 死亡・魂生成 (SOUL) | 3 | なし | SMALL_FLOOR | 済 |
| NBT 読み書き (NBT) | 7 | なし | SMALL_FLOOR | 済 |
| **合計** | **29** | 3ケース | |
