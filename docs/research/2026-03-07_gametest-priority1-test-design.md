# 第1優先グループ テスト設計

## 対象機能

1. 雇用・再雇用
2. 待機切替（砂糖）
3. フレンド判定 (`TameableUtil.isFriend`) / 攻撃対象判定 (`canTarget`)
4. 緊急状態判定 (`isEmergency`)

## API 構成（リファクタ後）

```
TameableUtil.isFriend(Tameable, LivingEntity)  -- テイム仲間判定（純粋ロジック）
LittleMaidEntity.canTarget(LivingEntity)       -- 攻撃対象判定（isFriend + ATTACK_PROHIBITED）
LittleMaidEntity.damage()                      -- ダメージ受付（enableFriendlyFire / blockDamageFromAttackProhibited）
```

- `isFriend`: テイム済みモブ・プレイヤー・同オーナーのテイムモブを「仲間」と判定
- `canTarget`: isFriend に加え、TargetTag の ATTACK_PROHIBITED も攻撃対象から除外
- `damage`: フレンド判定はコンフィグ `enableFriendlyFire` で制御、ATTACK_PROHIBITED は `blockDamageFromAttackProhibited` で制御

## 方針決定

### テスト種別

- **全テスト GameTest で実装**する。JUnit は Minecraft クラス階層のモックが困難なため見送り
- `TameableUtil.isFriend()` は JUnit 候補だったが、`Tameable` / `LivingEntity` のモック作成コストが高いため GameTest で実エンティティを使う

### コンフィグ制御

- **デフォルトコンフィグ前提**でテストを書く（方針A）
- コンフィグ分岐テスト（D-2, D-4）は **テスト前後でフィールドを直接書き換え+復元**（方針B）
- テスト開始時にコンフィグがデフォルトでない場合のエラーは許容する（割り切り）
- GameTest はバッチ内で逐次実行されるため、同一バッチなら並行問題は起きない

### プレイヤー生成

- FakePlayer を使用（Fabric / Forge でそれぞれ別実装）
- common に `@ExpectPlatform` メソッドを置き、fabric/forge の Impl で実装
- テストロジックは common に集約

### ファイル構成

```
common/.../gametest/
  GameTestHelper.java          -- @ExpectPlatform: createFakePlayer
  LMRBCommonTests.java         -- テストロジック本体（static メソッド群）

fabric/.../gametest/
  GameTestHelperImpl.java      -- FabricFakePlayer
  LMRBGameTests.java           -- FabricGameTest 実装、common に委譲

forge/.../gametest/
  GameTestHelperImpl.java      -- ForgeFakePlayer
  LMRBForgeGameTests.java      -- @GameTestHolder 実装、common に委譲
```

---

## 1. 雇用・再雇用

### テスト対象

- `LMInteractionHandler.handle()` -> `contract()`
- 対象タグ: `LMTags.Items.MAIDS_EMPLOYABLE`

### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| C-1 | 野良メイドさんを雇用できる | 野良メイドさん + プレイヤー | ケーキを持って interactMob | isTamed=true, ownerUuid=プレイヤー, movingMode=ESCORT, isContractMM=true, ケーキ1個消費 |
| C-2 | ストライキ中のメイドさんを再雇用できる | テイム済み + isStrike=true | ケーキを持って interactMob | isStrike=false, unpaidDays=0, テイム維持 |
| C-3 | 雇用アイテム以外では雇えない | 野良メイドさん | 石を持って interactMob | isTamed=false, 石消費なし |
| C-4 | オーナー以外はテイム済みメイドさんを操作できない | テイム済み | 別プレイヤーが interactMob | 結果=PASS, 状態変化なし |
| C-5 | スニーク中はインタラクションしない | テイム済み | スニーク状態で interactMob | 結果=PASS |

### 補足

- C-2 の前提条件セットアップ: `maid.setOwnerUuid(player.getUuid())` + `maid.setStrike(true)`
- C-4 は FakePlayer を2体生成する必要がある

---

## 2. 待機切替（砂糖）

### テスト対象

- `LMInteractionHandler.handle()` -> `changeState()`
- `TameableUtil.switchWait()`
- 回復: `maid.heal(config.health.healAmount)`

### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| W-1 | 砂糖で待機状態にできる | テイム済み + isSitting=false | 砂糖を持って interactMob | isSitting=true, 砂糖1個消費 |
| W-2 | 砂糖で待機解除できる | テイム済み + isSitting=true | 砂糖を持って interactMob | isSitting=false, 砂糖1個消費 |
| W-3 | 砂糖でHPが回復する | テイム済み + HP < maxHP | 砂糖を持って interactMob | HP が healAmount 分増加 |
| W-4 | ストライキ中は砂糖で操作できない | テイム済み + isStrike=true | 砂糖を持って interactMob | isSitting 変化なし |
| W-5 | オーナー以外は砂糖で操作できない | テイム済み | 別プレイヤーが砂糖で interactMob | 状態変化なし |

---

## 3. フレンド判定 / 攻撃対象判定

### テスト対象

- `TameableUtil.isFriend(Tameable, LivingEntity)`
- `LittleMaidEntity.canTarget(LivingEntity)`
- `LittleMaidEntity.damage()` のフレンド/ATTACK_PROHIBITED チェック

### 3a. isFriend（GameTest で実エンティティ使用）

| ID | テスト名 | self の状態 | target | 期待 |
|----|---------|-----------|--------|------|
| F-1 | ご主人はフレンド | オーナーあり | オーナー本人（FakePlayer） | true |
| F-2 | 同オーナーのテイム済みモブはフレンド | オーナーA | オーナーAのオオカミ | true |
| F-3 | テイム済みモブ全般はフレンド | オーナーあり | 別オーナーのテイム済みモブ | true |
| F-4 | オーナーあり時はプレイヤー全般フレンド | オーナーあり | 別プレイヤー（FakePlayer） | true |
| F-5 | 野生モブはフレンドでない | オーナーあり | 野生ゾンビ | false |
| F-6 | オーナーなし時はプレイヤーはフレンドでない | オーナーなし | プレイヤー | false |
| F-7 | オーナーなし + 野生モブ | オーナーなし | 野生ゾンビ | false |

### 3b. canTarget（GameTest）

| ID | テスト名 | 前提条件 | target | 期待 |
|----|---------|---------|--------|------|
| T-1 | ご主人を攻撃対象にしない | テイム済み | オーナー | false |
| T-2 | 同オーナーのテイム済みモブを攻撃対象にしない | テイム済み | 同オーナーのオオカミ | false |
| T-3 | テイム済みモブ全般を攻撃対象にしない | テイム済み | 別オーナーのテイム済みモブ | false |
| T-4 | ATTACK_PROHIBITED タグ付きを攻撃対象にしない | テイム済み + TargetTag設定 | タグ付きモブ | false |
| T-5 | 野生の敵対モブを攻撃対象にする | テイム済み | 野生ゾンビ | true |

### 3c. damage() のフレンド/ATTACK_PROHIBITED チェック（GameTest + コンフィグ変更）

| ID | テスト名 | コンフィグ | 操作 | 検証項目 |
|----|---------|----------|------|---------|
| D-1 | フレンドからのダメージはデフォルトで無効 | デフォルト (enableFriendlyFire=false) | オーナーがメイドさんを攻撃 | ダメージなし |
| D-2 | enableFriendlyFire=true ならフレンドからもダメージ受ける | enableFriendlyFire=true (要復元) | オーナーがメイドさんを攻撃 | ダメージあり |
| D-3 | ATTACK_PROHIBITED からのダメージはデフォルトで受ける | デフォルト (blockDamageFromAttackProhibited=false) | タグ付きモブがメイドさんを攻撃 | ダメージあり |
| D-4 | blockDamageFromAttackProhibited=true なら除外 | blockDamageFromAttackProhibited=true (要復元) | タグ付きモブがメイドさんを攻撃 | ダメージなし |

#### コンフィグ変更パターン

```java
var config = LMRBMod.getConfig();
var original = config.health.enableFriendlyFire;
try {
    config.health.enableFriendlyFire = true;
    // テスト実行
} finally {
    config.health.enableFriendlyFire = original;
}
```

---

## 4. 緊急状態判定

### テスト対象

```java
public boolean isEmergency() {
    LMRBConfig config = getConfig();
    return this.getHealth() / this.getMaxHealth() <= config.health.emergencyMaidHealthThreshold;
}
```

デフォルト: `emergencyMaidHealthThreshold = 0.5f`, `maxHealth = 20`

### テストケース

| ID | テスト名 | 操作 | 検証項目 |
|----|---------|------|---------|
| E-1 | HP閾値ちょうどで緊急状態 | setHealth(10) | isEmergency=true |
| E-2 | HP閾値未満で緊急状態 | setHealth(5) | isEmergency=true |
| E-3 | HP閾値超で通常状態 | setHealth(11) | isEmergency=false |
| E-4 | HP満タンで通常状態 | setHealth(20) | isEmergency=false |

---

## テスト共通のセットアップパターン

### メイドさんのスポーン

```java
var maid = context.spawnEntity(Registration.LITTLE_MAID_MOB.get(), pos);
```

### FakePlayer の生成（ExpectPlatform）

```java
// common
@ExpectPlatform
public static ServerPlayerEntity createFakePlayer(ServerWorld world) {
    throw new AssertionError();
}

// fabric impl
public static ServerPlayerEntity createFakePlayer(ServerWorld world) {
    return FakePlayer.get(world);
}
```

### テイム状態の設定

```java
maid.setOwnerUuid(player.getUuid());
```

### ストライキ状態の設定

```java
maid.setOwnerUuid(player.getUuid());
maid.setStrike(true);
```

### プレイヤーにアイテムを持たせて interactMob

```java
player.getInventory().setStack(player.getInventory().selectedSlot, new ItemStack(Items.CAKE));
maid.interactMob(player, Hand.MAIN_HAND);
```

### コンフィグの一時変更

```java
var config = LMRBMod.getConfig();
var original = config.health.enableFriendlyFire;
try {
    config.health.enableFriendlyFire = true;
    // テスト
} finally {
    config.health.enableFriendlyFire = original;
}
```

---

## 実装順序

1. **共通基盤**: ExpectPlatform (FakePlayer) + Fabric/Forge の委譲構造
2. **緊急状態テスト (E-1〜E-4)**: 最もシンプル。基盤の動作確認を兼ねる
3. **雇用テスト (C-1〜C-5)**: FakePlayer + interactMob の検証
4. **待機切替テスト (W-1〜W-5)**: 雇用テストのセットアップを再利用
5. **フレンド/canTarget テスト (F/T/D)**: 複数エンティティ + コンフィグ変更

## ケース数

| カテゴリ | ケース数 | コンフィグ変更 | 実装状況 |
|---------|---------|-------------|---------|
| 基本（スポーン） | 1 | なし | 済 |
| 雇用・再雇用 | 5 | なし | 済 |
| 待機切替 | 5 | なし | 済 |
| isFriend | 7 | なし | 済 |
| canTarget | 4 | なし | 済 (T-6 は FakePlayer の canTarget 挙動が不明なため保留) |
| damage (フレンド) | 2 | D-2 で必要 | 済 (D-3, D-4 は ATTACK_PROHIBITED セットアップが複雑なため保留) |
| 緊急状態 | 4 | なし | 済 |
| **合計** | **28** | 1ケースのみ |
