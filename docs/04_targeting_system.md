# ターゲティングシステム詳細仕様

## 📋 概要

Little Maid Rebirth のターゲティングシステムは、メイドさんの戦闘判断を司るシンプルで効率的なAIシステムです。複雑な多段階システムから3段階の優先度システムに改修され、理解しやすく操作しやすい戦術的思考能力を提供します。

---

## 🏗️ システム構成

### シンプルターゲティングアーキテクチャ

```
ターゲティングシステム
├── TargetingSystem.java          - 3段階優先度計算エンジン
│   ├── selectTarget()           - メイン選択ロジック
│   ├── determinePriority()      - 優先度判定
│   ├── isOverTargeted()         - 分散ターゲティング
│   └── 敵タグ判定メソッド群      - 特殊敵対応
├── LMTargetGoal.java            - AI行動への統合
├── TargetingConfig.java         - シンプル設定管理
├── IFFシステム                   - 敵味方識別（既存システム流用）
│   ├── IFF.java                 - 基本識別クラス
│   ├── IFFImpl.java             - 実装クラス
│   └── IFFTypeManager.java      - タイプ管理
└── 敵タグシステム                - 特殊な敵への対応
    ├── 攻撃禁止敵 (Wither等)
    ├── 接近禁止敵 (Creeper等)
    ├── 近距離攻撃禁止敵
    └── 遠距離攻撃禁止敵 (Enderman等)
```

---

## 🎯 3段階優先度システム

### シンプル優先度判定

ターゲティングシステムは以下の3段階で優先度を判定します：

```java
public enum TargetPriority {
    CRITICAL,  // 自分の身を守る
    HIGH,      // ご主人の身を守る  
    NORMAL     // 味方の身を守る・周囲の敵
}
```

### メインアルゴリズム

**場所**: `TargetingSystem.java:155-172`

```java
public static Optional<MobEntity> selectTarget(
    Maid maid, List<Mob> enemies, @Nullable Master master, List<Maid> otherMaids) {
    
    int maxAttackers = calculateMaxAttackers(otherMaids);
    
    // Stream処理による効率的なターゲット選択
    Optional<Mob> bestTarget = enemies.stream()
        .filter(enemy -> !shouldAvoidDangerous(enemy, maid))           // 1. 危険敵回避判定
        .filter(enemy -> !isOverTargeted(enemy, otherMaids, maxAttackers)) // 2. 分散ターゲティング
        .map(enemy -> new TargetCandidate(enemy, determinePriority(enemy, maid, master, otherMaids))) // 3. 優先度判定
        .filter(candidate -> candidate.priority != null)              // 4. 攻撃可能敵のみ
        .min(Comparator                                              // 5. 優先度→距離順ソート
            .comparing((TargetCandidate c) -> c.priority)
            .thenComparingDouble(c -> c.enemy.getPosition().distanceTo(maid.getPosition())))
        .map(candidate -> candidate.enemy);
        
    return bestTarget.map(Mob::getMob);
}
```

---

## 🔢 各段階の詳細実装

### 1. CRITICAL: 自分の身を守る

**場所**: `TargetingSystem.java:187-190`

最優先で自分を攻撃した敵をターゲットします：

```java
// CRITICAL: 自分の身を守る
if (maid.isAttackedBy(enemy)) {
    return TargetPriority.CRITICAL;
}
```

### 2. HIGH: ご主人の身を守る

**場所**: `TargetingSystem.java:192-195`

ご主人を攻撃した敵、またはご主人が攻撃した敵を優先します：

```java
// HIGH: ご主人の身を守る
if (master != null && (master.isAttackedBy(enemy) || master.isTargeting(enemy) || enemy.isAttackedBy(master))) {
    return TargetPriority.HIGH;
}
```

### 3. NORMAL: 味方の身を守る・周囲の敵

**場所**: `TargetingSystem.java:197-215`

他のメイドさんを攻撃した敵、または警戒範囲内の敵対モブを対象とします：

```java
// NORMAL: 味方の身を守る・周囲の敵
for (Maid otherMaid : otherMaids) {
    if (otherMaid.isAttackedBy(enemy) || otherMaid.isTargeting(enemy)) {
        return TargetPriority.NORMAL;
    }
}

// 先制攻撃対象（距離内の敵対モブ、攻撃禁止敵以外）
if (enemy.isEnemy()) {
    // 攻撃禁止敵は攻撃対象外
    if (hasAttackProhibitedTag(enemy)) {
        return null;
    }
    
    float distanceToMaid = (float) enemy.getPosition().distanceTo(maid.getPosition());
    if (distanceToMaid <= TargetingConfig.getAlertRange()) {
        return TargetPriority.NORMAL;
    }
}
```

---

## 🏷️ 敵タグシステム

### 4種類の敵タグ

**場所**: `TargetingSystem.java:34-39`

```java
public enum EnemyTag {
    ATTACK_PROHIBITED,       // 攻撃禁止（一切攻撃しない）
    APPROACH_PROHIBITED,     // 接近禁止（弓なら遠距離攻撃可能）
    MELEE_ATTACK_PROHIBITED, // 近距離攻撃禁止（剣では攻撃不可）
    RANGED_ATTACK_PROHIBITED // 遠距離攻撃禁止（弓では攻撃不可）
}
```

### 敵タグ判定実装

```java
// 攻撃禁止敵（Wither、Ender Dragon、Warden等）
private static boolean hasAttackProhibitedTag(Mob enemy) {
    EntityType<?> type = enemy.getMob().getType();
    return type == EntityType.WITHER ||
           type == EntityType.ENDER_DRAGON ||
           type == EntityType.WARDEN;
}

// 接近禁止敵（Creeper等）
private static boolean hasApproachProhibitedTag(Mob enemy) {
    EntityType<?> type = enemy.getMob().getType();
    return type == EntityType.CREEPER;
}

// 近距離攻撃禁止敵（WitherSkeleton、Ravager等）
private static boolean hasMeleeAttackProhibitedTag(Mob enemy) {
    EntityType<?> type = enemy.getMob().getType();
    return type == EntityType.WITHER_SKELETON ||
           type == EntityType.RAVAGER;
}

// 遠距離攻撃禁止敵（Enderman等）
private static boolean hasRangedAttackProhibitedTag(Mob enemy) {
    return enemy.getMob().getType() == EntityType.ENDERMAN;
}
```

---

## 🎯 分散ターゲティングシステム

### 集中攻撃防止ロジック

**場所**: `TargetingSystem.java:229-237`

```java
private static boolean isOverTargeted(Mob enemy, List<Maid> otherMaids, int maxAttackers) {
    long currentAttackers = otherMaids.stream()
        .filter(maid -> maid.isTargeting(enemy))
        .filter(maid -> maid.getCombatType() != Mode.BattleModeType.NONE)
        .filter(maid -> !maid.isInjured())
        .count();
        
    return currentAttackers >= maxAttackers;
}
```

### 最大攻撃者数計算

**場所**: `TargetingSystem.java:284-288`

```java
private static int calculateMaxAttackers(List<Maid> otherMaids) {
    int totalMaids = otherMaids.size() + 1; // 自分も含める
    int distributedCount = (int) Math.ceil(totalMaids * TargetingConfig.getDistributionRatio());
    return Math.min(TargetingConfig.getMaxAttackersPerEnemy(), distributedCount);
}
```

---

## 🚨 避難システム

### 危険敵からの自動避難

**場所**: `TargetingSystem.java:335-342` / `LMTargetGoal.java:77-84`

```java
// 避難が必要かの判定
public static boolean needsEvacuation(Maid maid, List<Mob> enemies) {
    return enemies.stream()
        .anyMatch(enemy -> {
            double distance = maid.getPosition().distanceTo(enemy.getPosition());
            return (hasAttackProhibitedTag(enemy) || hasApproachProhibitedTag(enemy))
                && distance < TargetingConfig.getDangerousAvoidDistance();
        });
}

// 実際の避難処理（LMTargetGoal.java内）
if (TargetingSystem.needsEvacuation(maidWrapper, enemies)) {
    TargetingSystem.getDangerousEnemies(maidWrapper, enemies)
        .forEach(mob -> this.maid.addFleeEntity(mob.getMob(), e ->
            !e.isAlive()
            || this.maid.squaredDistanceTo(e) > (TargetingConfig.getDangerousAvoidDistance() + 4)
               * (TargetingConfig.getDangerousAvoidDistance() + 4))
        );
}
```

---

## ⚔️ LMTargetGoal - AI行動統合

### シンプルなターゲット選択実装

**場所**: `/entity/goal/LMTargetGoal.java:43-95`

```java
private boolean targeting() {
    // 範囲内に敵がいるかチェック
    var aroundMobs = getAroundMobs();
    if (aroundMobs.isEmpty()) {
        this.maid.setTarget(null);
        return false;
    }
    var aroundMaids = getAroundMaids();
    
    // 3段階優先度システムでターゲット選択
    var target = TargetingSystem.selectTarget(
        new TargetingSystem.Maid(this.maid),
        aroundMobs.stream()
            .map(mob -> new TargetingSystem.Mob(
                mob,
                this.maid.identify(mob)
                    .map(tag -> {
                        if (this.maid.isBloodSuck()) {
                            return true;
                        } else {
                            return tag == IFFTag.ENEMY;
                        }
                    })
                    .orElse(this.maid.isBloodSuck())
            )).toList(),
        TameableUtil.getTameOwner(this.maid).map(TargetingSystem.Master::new).orElse(null),
        aroundMaids.stream().map(TargetingSystem.Maid::new).toList()
    );
    
    // 危険敵からの避難処理
    var enemies = aroundMobs.stream()
        .map(mob -> new TargetingSystem.Mob(mob, this.maid.identify(mob).map(tag -> tag == IFFTag.ENEMY).orElse(false)))
        .toList();
    var maidWrapper = new TargetingSystem.Maid(this.maid);
    if (TargetingSystem.needsEvacuation(maidWrapper, enemies)) {
        TargetingSystem.getDangerousEnemies(maidWrapper, enemies)
            .forEach(mob -> this.maid.addFleeEntity(mob.getMob(), e ->
                !e.isAlive()
                || this.maid.squaredDistanceTo(e) > (TargetingConfig.getDangerousAvoidDistance() + 4)
                   * (TargetingConfig.getDangerousAvoidDistance() + 4))
            );
    }
    
    // ターゲット設定
    if (target.isPresent()) {
        this.target = target.get();
        this.maid.setTarget(target.get());
        return true;
    }
    
    this.maid.setTarget(null);
    return false;
}
```

---

## ⚙️ TargetingConfig - シンプル設定システム

### 設定項目一覧

**場所**: `/util/TargetingConfig.java` / `/config/LMRBConfig.java:Target`

```java
public static class Target {
    // 距離関連設定 (3個)
    public int alertRange = 16;              // 警戒範囲（敵検出・先制攻撃範囲）
    public int combatRange = 8;              // 戦闘範囲（実際の戦闘行動範囲）
    public int dangerousAvoidDistance = 8;   // 危険敵回避距離
    
    // 分散ターゲティング設定 (2個)
    public double distributionRatio = 0.5;   // 分散比率（メイドさん数の50%）
    public int maxAttackersPerEnemy = 2;     // 1体あたり最大攻撃者数
    
    // 体力関連設定 (2個)
    public float injuredThreshold = 0.5f;    // 負傷判定閾値（体力50%以下）
    public int attackedByValidTicks = 100;   // 攻撃判定有効時間（5秒間）
}
```

**合計7個の設定項目**により、シンプルで理解しやすい設定システムを実現しています。

---

## 📈 改修による改善効果

### コード構造の改善

- **ファイル構成**: TargetingSystem.java (358行、11メソッド)
- **メソッド構成**: 明確な責任分離による11個の専門メソッド
- **設定項目**: 7個の直感的な設定項目
- **計算複雑度**: O(n×m) - n:敵数、m:メイドさん数

### メンテナンス性の向上

- **バグの原因特定が容易**: シンプルな3段階判定とStream処理
- **新機能追加が簡単**: 明確な責任分離とフィルタリング設計
- **テストが書きやすい**: 個別機能の独立性と純粋関数的設計
- **設定変更の影響が予測しやすい**: 少ない設定項目と明確な役割

### プレイヤー体験の向上

- **メイドさんの行動が予測しやすい**: 明確な優先度階層（自分→ご主人→味方→周囲）
- **設定項目が理解しやすい**: 直感的な設定名称と単位
- **意図した行動を実現しやすい**: シンプルな制御システム

---

## 📊 計算複雑度詳細

### selectTarget()メソッドの分析

```java
enemies.stream()  // n個の敵に対して
    .filter(enemy -> !shouldAvoidDangerous(enemy, maid))           // O(1) per enemy
    .filter(enemy -> !isOverTargeted(enemy, otherMaids, maxAttackers))  // O(m) per enemy
    .map(enemy -> new TargetCandidate(enemy, determinePriority(enemy, maid, master, otherMaids))) // O(m) per enemy
    .filter(candidate -> candidate.priority != null)              // O(1) per enemy
    .min(...)  // O(n) for finding minimum
```

**全体の計算複雑度: O(n × m)**
- n: 周囲の敵の数（通常5-20体程度）
- m: 周囲のメイドさんの数（通常1-10体程度）

実際のゲーム環境では非常に効率的に動作します。

---

## 🔧 拡張性

### 敵タグシステムの拡張

現在は個別メソッドでハードコード実装していますが、将来的には以下の拡張が可能です：

```java
// TODO: 以下のような拡張を検討
// 1. IFFシステムとの統合
// 2. 設定ファイルでの敵タグ定義
// 3. データパックでの敵タグ拡張
```

### 新しい優先度階層の追加

```java
public enum TargetPriority {
    EMERGENCY,   // 新しい最高優先度（例：爆発直前のクリーパー）
    CRITICAL,    // 自分の身を守る
    HIGH,        // ご主人の身を守る
    NORMAL       // 味方の身を守る・周囲の敵
}
```

---

## 📋 まとめ

ターゲティングシステムは複雑な多段階システムから理解しやすい3段階システムに改修されました：

### ✅ 実装済み機能

- **3段階優先度システム**: CRITICAL > HIGH > NORMAL
- **敵タグシステム**: 4種類の特殊対応（攻撃禁止、接近禁止、等）
- **分散ターゲティング**: 集中攻撃防止機能
- **避難システム**: 危険敵からの自動回避
- **シンプル設定**: 7個の直感的な設定項目

### 🎯 戦術的価値

- **明確な優先度**: 自分→ご主人→味方→周囲の敵
- **武器種別対応**: 弓/剣に応じた敵タグ判定
- **協調戦闘**: 分散ターゲティングによる効率的戦力配分
- **安全性重視**: 危険敵からの自動避難

### 🚀 技術的優秀性

- **パフォーマンス**: O(n×m)の効率的な計算
- **可読性**: Stream処理による理解しやすいコード構造
- **テスト性**: 純粋関数的な独立機能単位
- **拡張性**: 将来的な機能追加に対応した設計

このシンプルなターゲティングシステムは、プレイヤーが理解しやすく、開発者がメンテナンスしやすい、実用的で効率的な設計となっています。