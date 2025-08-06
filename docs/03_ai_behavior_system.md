# AI行動システム詳細仕様

## 📋 概要

Little Maid Rebirth のAI行動システムは、Minecraftの標準的なGoalシステムを基盤として、高度で実用的なAI行動を実現するシステムです。階層化された優先度管理と専門的なモードシステムにより、メイドさんが状況に応じた適切な判断と行動を行います。

---

## 🏗️ システム構成

### 二層構造のAI設計

```
AI行動システム
├── Goal（行動ゴール）システム    - 基本的な行動単位
│   ├── 緊急行動（優先度0-2）
│   ├── 生存行動（優先度3-5）  
│   ├── 戦闘・移動行動（優先度6-9）
│   └── 日常行動（優先度10-15）
│
└── Mode（作業モード）システム     - 専門的な作業・戦闘
    ├── 戦闘モード（弓兵、剣士、回復等）
    └── 作業モード（料理、松明設置等）
```

---

## 🎯 Goal（行動ゴール）システム

### 優先度階層

メイドさんの行動は16個のGoalが優先度順に管理されます：

#### **最高優先度（緊急行動）**
```java
// 優先度0: 緊急テレポート - 体力低下時の避難
this.goalSelector.add(0, new LMEmergencyTeleportGoal(this));

// 優先度1: 水泳 - 溺死防止
this.goalSelector.add(1, new SwimGoal(this));

// 優先度2: ドア操作 - 移動経路確保
this.goalSelector.add(2, new LongDoorInteractGoal(this, true));
```

#### **高優先度（生存行動）**
```java
// 優先度3: 自己回復 - 体力管理
this.goalSelector.add(3, new LMHealMyselfGoal(this));

// 優先度4: 給料収集 - リソース確保
this.goalSelector.add(4, new LMCollectSalaryFromContainerGoal(this));

// 優先度5: 待機 - ご主人の指示
this.goalSelector.add(5, new WaitGoal(this));
```

#### **中優先度（戦闘・移動行動）**
```java
// 優先度6: 通常テレポート - 距離調整
this.goalSelector.add(6, new LMTeleportTameOwnerGoal(this));

// 優先度7: 危険回避 - 脅威からの逃避
this.goalSelector.add(7, new FleeEntityGoal<>(this, predicate, speed, slowSpeed));

// 優先度8: モード実行 - 専門作業・戦闘
this.goalSelector.add(8, new ModeWrapperGoal(this));

// 優先度9: 追従 - ご主人への追従
this.goalSelector.add(9, new HasMMFollowTameOwnerGoal(this, speed, minDistance, maxDistance));
```

#### **低優先度（日常行動）**
```java
// 優先度10: 給料反応 - 給料アイテムへの注目
this.goalSelector.add(10, new FollowAtHeldItemGoal(this, speed, minDistance, maxDistance));

// 優先度11: アイテム保管 - 不要アイテムの格納
this.goalSelector.add(11, new LMStoreItemToContainerGoal(this));

// 優先度12: アイテム収集 - ドロップアイテム回収
this.goalSelector.add(12, new LMMoveToDropItemGoal(this));

// 優先度13: 娯楽 - 雪遊び
this.goalSelector.add(13, new PlaySnowGoal(this));

// 優先度14: 探索 - レッドストーン追跡
this.goalSelector.add(14, new RedstoneTraceGoal(this));

// 優先度15: 徘徊 - 野良状態での探索
this.goalSelector.add(15, new WanderAroundFarGoal(this, speed));
```

---

## 🔥 主要Goal実装詳細

### 1. LMHealMyselfGoal - 自己回復システム

**場所**: `/entity/goal/LMHealMyselfGoal.java`

体力管理の核となるゴールで、給料アイテムを消費して回復を行います：

```java
public class LMHealMyselfGoal extends Goal {
    private final LittleMaidEntity maid;
    private int healCooldown = 0;
    
    @Override
    public boolean canStart() {
        // 体力満タン時は実行しない
        if (maid.getHealth() >= maid.getMaxHealth()) return false;
        
        // 無敵時間中で体力に余裕がある場合は遅延
        if (maid.hurtTime > 0 && maid.getHealth() > maid.getMaxHealth() * 0.3f) {
            return false;
        }
        
        // クールダウン中は実行しない
        if (healCooldown > 0) return false;
        
        // 給料アイテムを持っているかチェック
        return hasSalaryItem();
    }
    
    @Override
    public void tick() {
        if (consumeSalaryItem()) {
            maid.heal(2.0f);  // 2ハート回復
            healCooldown = 60;  // 3秒のクールダウン
        }
    }
}
```

### 2. PlaySnowGoal - 雪遊び行動

**場所**: `/entity/goal/PlaySnowGoal.java`

メイドさんの愛らしい行動の一つで、複雑なステートマシンを実装：

```java
public class PlaySnowGoal extends Goal {
    private enum State {
        MAKING_SNOWBALL,    // 雪玉作成
        SEARCHING_TARGET,   // ターゲット探索
        THROWING_SNOWBALL   // 雪玉投擲
    }
    
    private State currentState = State.MAKING_SNOWBALL;
    private int stateTicks = 0;
    private LivingEntity target;
    
    @Override
    public boolean canStart() {
        // 朝～昼の時間帯のみ
        long time = maid.getWorld().getTimeOfDay() % 24000;
        if (time < 0 || time > 12000) return false;
        
        // 雪ブロック上でのみ実行
        BlockState blockBelow = maid.getWorld().getBlockState(maid.getBlockPos().down());
        return blockBelow.isOf(Blocks.SNOW_BLOCK);
    }
    
    @Override
    public void tick() {
        switch (currentState) {
            case MAKING_SNOWBALL:
                // しゃがんで雪を集める演出
                maid.setSneaking(true);
                if (++stateTicks >= 40) {  // 2秒間
                    transitionTo(State.SEARCHING_TARGET);
                }
                break;
                
            case SEARCHING_TARGET:
                // 視界内の生物を探す
                target = findNearbyLivingEntity();
                if (target != null) {
                    transitionTo(State.THROWING_SNOWBALL);
                }
                break;
                
            case THROWING_SNOWBALL:
                // ターゲットに向けて投げる
                throwSnowballAt(target);
                this.stop();
                break;
        }
    }
}
```

### 3. LMCollectSalaryFromContainerGoal - 給料収集

**場所**: `/entity/goal/LMCollectSalaryFromContainerGoal.java`

コンテナから給料アイテムを自動回収するシステム：

```java
public class LMCollectSalaryFromContainerGoal extends Goal {
    private final ProcessDivider processSteps;  // 処理分散
    private BlockPos targetContainer;
    
    @Override
    public boolean canStart() {
        // 給料アイテムが必要な状況かチェック
        if (maid.hasEnoughSalary()) return false;
        
        // 利用可能なコンテナを探索
        targetContainer = findNearbyContainer();
        return targetContainer != null;
    }
    
    @Override
    public void tick() {
        processSteps.process(() -> {
            // ステップ1: コンテナに接近
            if (!maid.isNear(targetContainer)) {
                maid.getNavigation().startMovingTo(targetContainer.getX(), targetContainer.getY(), targetContainer.getZ(), 1.0);
                return false;
            }
            
            // ステップ2: アクセス（しゃがんで開ける演出）
            maid.setSneaking(true);
            
            // ステップ3: アイテム回収処理
            return collectItemsFromContainer();
        });
    }
}
```

---

## ⚔️ Mode（作業モード）システム

### AbstractBattleMode - 戦闘モード基底クラス

**場所**: `/entity/mode/AbstractBattleMode.java`

全ての戦闘モードの基底クラス：

```java
public abstract class AbstractBattleMode extends AbstractMode {
    protected LivingEntity target;          // 現在のターゲット
    protected ItemStack weaponStack;        // 武器スタック
    protected Object weapon;                // 武器インスタンス
    
    @Override
    public boolean shouldExecute(LittleMaidEntity maid) {
        // 基本条件チェック
        if (!hasValidWeapon(maid)) return false;
        if (!hasTarget(maid)) return false;
        if (!canReachTarget(maid)) return false;
        
        return true;
    }
    
    @Override
    public void tick(LittleMaidEntity maid) {
        updateTarget(maid);
        updateWeapon(maid);
        
        if (shouldExecute(maid)) {
            executeAttack(maid);
        }
    }
    
    // 各戦闘モードで実装
    protected abstract void executeAttack(LittleMaidEntity maid);
}
```

### ArcherMode - 弓兵モード

**場所**: `/entity/mode/ArcherMode.java`

遠距離戦闘の専門モード：

```java
public class ArcherMode extends AbstractBattleMode {
    private int drawTime = 0;
    private int shootCooldown = 0;
    
    @Override
    public boolean shouldExecute(LittleMaidEntity maid) {
        if (!super.shouldExecute(maid)) return false;
        
        // 弓を持っているかチェック
        ItemStack weapon = maid.getMainHandStack();
        if (!weapon.getItem() instanceof BowItem && !weapon.getItem() instanceof CrossbowItem) {
            return false;
        }
        
        // 射程距離内かチェック
        double distance = maid.distanceTo(target);
        return distance >= 4.0 && distance <= 20.0;
    }
    
    @Override
    protected void executeAttack(LittleMaidEntity maid) {
        double distance = maid.distanceTo(target);
        
        // 射撃姿勢
        maid.getLookControl().lookAt(target);
        maid.setAiming(true);
        
        // 弓を引く
        if (drawTime < 20) {
            drawTime++;
            return;
        }
        
        // 射撃実行
        if (shootCooldown <= 0) {
            shootArrow(maid, target);
            drawTime = 0;
            shootCooldown = 20;  // 1秒のクールダウン
        }
        
        shootCooldown--;
    }
    
    private void shootArrow(LittleMaidEntity maid, LivingEntity target) {
        ArrowEntity arrow = new ArrowEntity(maid.getWorld(), maid);
        
        // 弾道計算
        double dx = target.getX() - maid.getX();
        double dy = target.getBodyY(0.3) - arrow.getY();
        double dz = target.getZ() - maid.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        arrow.setVelocity(dx, dy + distance * 0.2, dz, 1.6f, 12.0f);
        maid.getWorld().spawnEntity(arrow);
    }
}
```

### CookingMode - 料理モード

**場所**: `/entity/mode/CookingMode.java`

最も複雑な作業モードの一つ：

```java
public class CookingMode extends AbstractMode {
    private static final Map<LittleMaidEntity, BlockPos> USED_FURNACE_MAP = new HashMap<>();
    private BlockPos currentFurnace;
    private CookingState state = CookingState.SEARCHING_FURNACE;
    
    private enum CookingState {
        SEARCHING_FURNACE,    // かまど探索
        MOVING_TO_FURNACE,    // かまど接近
        COOKING_ITEMS,        // 料理実行
        COLLECTING_RESULTS    // 結果回収
    }
    
    @Override
    public boolean shouldExecute(LittleMaidEntity maid) {
        // 料理可能なアイテムを持っているかチェック
        if (!hasCookableItems(maid)) return false;
        
        // 燃料を持っているかチェック
        if (!hasFuelItems(maid)) return false;
        
        return true;
    }
    
    @Override
    public void tick(LittleMaidEntity maid) {
        switch (state) {
            case SEARCHING_FURNACE:
                currentFurnace = findAvailableFurnace(maid);
                if (currentFurnace != null) {
                    USED_FURNACE_MAP.put(maid, currentFurnace);
                    state = CookingState.MOVING_TO_FURNACE;
                }
                break;
                
            case MOVING_TO_FURNACE:
                if (maid.isNear(currentFurnace, 2.0)) {
                    maid.setSneaking(true);  // アクセス演出
                    state = CookingState.COOKING_ITEMS;
                } else {
                    maid.getNavigation().startMovingTo(currentFurnace);
                }
                break;
                
            case COOKING_ITEMS:
                if (processCooking(maid)) {
                    state = CookingState.COLLECTING_RESULTS;
                }
                break;
                
            case COLLECTING_RESULTS:
                collectResults(maid);
                resetState();
                break;
        }
    }
    
    private boolean processCooking(LittleMaidEntity maid) {
        FurnaceBlockEntity furnace = getFurnaceEntity(currentFurnace);
        
        // 材料投入
        if (furnace.getStack(0).isEmpty()) {
            ItemStack cookable = findCookableItem(maid);
            if (!cookable.isEmpty()) {
                furnace.setStack(0, cookable.split(1));
                maid.getInventory().removeStack(cookable);
            }
        }
        
        // 燃料投入
        if (furnace.getStack(1).isEmpty()) {
            ItemStack fuel = findFuelItem(maid);
            if (!fuel.isEmpty()) {
                furnace.setStack(1, fuel.split(1));
                maid.getInventory().removeStack(fuel);
            }
        }
        
        // 結果が出たかチェック
        return !furnace.getStack(2).isEmpty();
    }
    
    @Override
    public void resetTask(LittleMaidEntity maid) {
        // かまど占有を解除
        USED_FURNACE_MAP.remove(maid);
        
        // 未回収のアイテムがあれば回収
        if (currentFurnace != null) {
            collectAllItemsFromFurnace(maid, currentFurnace);
        }
        
        resetState();
    }
}
```

---

## 🔄 競合制御システム

### conflictsWith() による同時実行防止

各GoalはControl型のEnumSetで制御対象を宣言：

```java
public enum Control {
    MOVE,    // 移動制御
    LOOK,    // 視線制御  
    TARGET   // ターゲット制御
}

@Override
public EnumSet<Control> getControls() {
    return EnumSet.of(Control.MOVE, Control.LOOK);
}
```

同じ制御を要求するゴールは同時実行されません。

### 実行制御メソッド

```java
public abstract class Goal {
    // 新しい行動の開始条件
    public abstract boolean canStart();
    
    // 実行中の行動の継続条件（通常はcanStart()と異なる判定）
    public boolean shouldContinue() {
        return canStart();  // デフォルト実装
    }
    
    // 行動開始時の初期化
    public void start() {}
    
    // 毎ティックの実行処理
    public void tick() {}
    
    // 行動終了時のクリーンアップ
    public void stop() {}
}
```

---

## 🧠 ModeWrapperGoal - モード統合

**場所**: `/entity/goal/ModeWrapperGoal.java`

GoalシステムとModeシステムを橋渡しする重要なクラス：

```java
public class ModeWrapperGoal extends Goal {
    private final LittleMaidEntity maid;
    
    @Override
    public boolean canStart() {
        AbstractMode mode = maid.getMode();
        return mode != null && mode.shouldExecute(maid);
    }
    
    @Override
    public boolean shouldContinue() {
        AbstractMode mode = maid.getMode();
        return mode != null && mode.shouldExecute(maid);
    }
    
    @Override
    public void start() {
        AbstractMode mode = maid.getMode();
        if (mode != null) {
            mode.startExecuting(maid);
        }
    }
    
    @Override
    public void tick() {
        AbstractMode mode = maid.getMode();
        if (mode != null) {
            mode.tick(maid);
        }
    }
    
    @Override
    public void stop() {
        AbstractMode mode = maid.getMode();
        if (mode != null) {
            mode.resetTask(maid);
        }
    }
}
```

---

## 📈 パフォーマンス最適化

### ProcessDivider - 処理分散システム

重い処理を複数ティックに分散して実行：

```java
public class ProcessDivider {
    private final List<Supplier<Boolean>> steps = new ArrayList<>();
    private int currentStep = 0;
    
    public void process(Supplier<Boolean> step) {
        if (currentStep < steps.size()) {
            if (steps.get(currentStep).get()) {
                currentStep++;
            }
        }
    }
    
    public void reset() {
        currentStep = 0;
    }
}
```

### 効率的な探索システム

```java
public class BlockFinderPD {
    // 非同期でブロックを探索
    public static BlockPos findNearbyBlock(Entity entity, Predicate<BlockState> predicate, int range) {
        // 段階的に範囲を広げて探索
        for (int r = 1; r <= range; r += 2) {
            BlockPos found = searchInRange(entity.getBlockPos(), predicate, r);
            if (found != null) return found;
        }
        return null;
    }
}
```

---

## 🔧 拡張性と新Goal追加

### 新しいGoalの作成例

```java
public class CustomGoal extends Goal {
    private final LittleMaidEntity maid;
    
    public CustomGoal(LittleMaidEntity maid) {
        this.maid = maid;
        // 使用する制御を指定
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }
    
    @Override
    public boolean canStart() {
        // 実行条件をチェック
        return /* 条件 */;
    }
    
    @Override
    public boolean shouldContinue() {
        // 継続条件（通常はcanStart()より緩い条件）
        return /* 継続条件 */;
    }
    
    @Override
    public void tick() {
        // 毎ティックの処理
    }
}
```

### Goalの登録

```java
@Override
protected void initGoals() {
    // 適切な優先度で追加
    this.goalSelector.add(priority, new CustomGoal(this));
}
```

---

## 📋 まとめ

AI行動システムは以下の特徴を持つ優秀な設計です：

### ✅ 優秀な点
- **階層化された優先度管理**: 生存→戦闘→日常の明確な順序
- **競合回避システム**: 複数行動の適切な制御
- **モード統合**: 専門的な作業との柔軟な連携
- **パフォーマンス対策**: 重い処理の分散実行
- **拡張性**: 新しいGoalの追加が容易

### 🚀 今後の発展可能性
- **学習機能**: プレイヤーの行動パターンを学習
- **感情システム**: 状況に応じた感情的な反応
- **より高度な協調**: 複数メイドさんでのチーム戦術
- **環境認識**: より詳細な状況判断能力

このAI行動システムは、Minecraft MODにおけるAI実装の優秀な例として、他のプロジェクトでも参考にできる高度で実用的な設計となっています。