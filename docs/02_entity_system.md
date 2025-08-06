# エンティティシステム詳細仕様

## 📋 概要

Little Maid Rebirth のエンティティシステムは、メイドさんの核となる機能を実装するシステムです。単一のエンティティクラスが複数のインターフェースを実装し、委譲パターンによって各機能を分離・管理しています。

---

## 🏗️ クラス構造

### LittleMaidEntity.java
**場所**: `/entity/LittleMaidEntity.java`  
**継承**: `TameableEntity`

#### 実装インターフェース
```java
public class LittleMaidEntity extends TameableEntity implements
    EntitySpawnExtension,  // スポーン拡張機能
    HasInventory,          // インベントリ機能  
    Contractable,          // 契約機能
    HasMode,               // モード機能
    HasIFF,                // 敵味方識別機能
    AimingPoseable,        // エイミング機能
    IHasMultiModel,        // マルチモデル対応
    SoundPlayable,         // 音声再生機能
    HasMovingMode,         // 移動モード機能
    CrossbowUser,          // クロスボウ使用機能
    SalaryBoxPosListener   // 給料箱位置リスナー
```

#### 委譲オブジェクト
各機能を専用クラスに委譲することで、単一責任原則を維持：

```java
// インベントリ機能
private final LMHasInventory littleMaidInventory;

// 契約機能  
private final LMItemContractable itemContractable;

// モード機能
private final HasModeImpl hasModeImpl;

// マルチモデル機能
private final MultiModel multiModel;

// 音声再生機能
private final SoundPlayer soundPlayer;
```

---

## 📊 データ管理システム

### DataTracker による状態管理

メイドさんの状態は DataTracker を使用してクライアント・サーバー間で同期されます：

```java
// フラグ管理（単一のintで複数の状態を管理）
private static final TrackedData<Integer> LMM_FLAGS = 
    DataTracker.registerData(LittleMaidEntity.class, TrackedDataHandlerRegistry.INTEGER);

// 個別状態
private static final TrackedData<String> MODE_NAME = ...;           // 現在のモード名
private static final TrackedData<Byte> MOVING_MODE = ...;           // 移動モード
private static final TrackedData<Boolean> CHARGING = ...;           // チャージ状態
private static final TrackedData<Boolean> ACCELERATE = ...;         // 加速状態
private static final TrackedData<Byte> MASTER_STANCE = ...;         // マスタースタンス
```

#### フラグシステム
LMM_FLAGS は複数の boolean 状態をビットフラグで効率的に管理：

```java
public static final int FLAG_WAITING = 0;        // 待機
public static final int FLAG_AIMING = 1;         // エイミング  
public static final int FLAG_BEGGING = 2;        // ねだり
public static final int FLAG_BLOOD_SUCKING = 3;  // 吸血
public static final int FLAG_STRIKING = 4;       // ストライキ
public static final int FLAG_PLAYING_SNOW = 5;   // 雪遊び
```

### NBT による永続化

メイドさんの全データは NBT 形式でワールドファイルに保存されます：

```java
@Override
public void writeCustomDataToNbt(NbtCompound nbt) {
    super.writeCustomDataToNbt(nbt);
    
    // バージョン情報
    nbt.putString("LMVersion", getLMVersion());
    
    // 基本データ
    littleMaidInventory.writeToNbt(nbt);
    nbt.putInt("LMExperience", getExperience());
    
    // 状態データ
    nbt.putBoolean("LMWaiting", getWaiting());
    nbt.putByte("LMMovingMode", (byte) getMovingMode().ordinal());
    nbt.putByte("LMMasterStance", (byte) getMasterStance().ordinal());
    
    // 機能別データ
    itemContractable.writeToNbt(nbt);
    hasModeImpl.writeToNbt(nbt);
    // ... その他のデータ
}
```

---

## 🎭 主要機能実装

### インベントリシステム (LMHasInventory)

**場所**: `/entity/util/LMHasInventory.java`

メイドさん専用の18スロットインベントリを実装：

```java
public class LMHasInventory implements HasInventory {
    private static final int INVENTORY_SIZE = 18;
    private final DefaultedList<ItemStack> inventory;
    
    // メイン・オフハンド装備スロット
    private final DefaultedList<ItemStack> handItems;
    
    // 防具スロット  
    private final DefaultedList<ItemStack> armorItems;
}
```

#### スロット構成
- **0-17**: メイドインベントリ（18スロット）
- **メイン・オフハンド**: 武器・道具用
- **防具スロット**: 頭・胴・脚・足装備

### 契約システム (LMItemContractable)

**場所**: `/entity/util/LMItemContractable.java` 

給料システムによるメイドさんとの契約関係を管理：

```java
public class LMItemContractable implements Contractable {
    private ItemStack contractStack = ItemStack.EMPTY;  // 契約アイテム
    private ItemStack salaryStack = ItemStack.EMPTY;    // 給料アイテム
    private int missPayDay = 0;                         // 未払い日数
    
    // 契約状態の判定
    @Override
    public boolean hasContract() {
        return !contractStack.isEmpty();
    }
    
    // 給料の自動取得
    public boolean autoGetSalary(LittleMaidEntity maid) {
        // 給料箱から自動回収処理
    }
}
```

### モードシステム (HasModeImpl)

**場所**: `/entity/util/HasModeImpl.java`

戦闘・作業モードの管理：

```java
public class HasModeImpl implements HasMode {
    private String modeName = "";
    private AbstractMode mode;
    
    @Override
    public void setMode(String modeName) {
        this.modeName = modeName;
        this.mode = LMRBModeRegistry.getMode(modeName);
    }
    
    // モードの実行
    public void executeMode(LittleMaidEntity maid) {
        if (mode != null && mode.shouldExecute(maid)) {
            mode.tick(maid);
        }
    }
}
```

---

## 👤 MaidSoulEntity - 魂システム

**場所**: `/entity/MaidSoulEntity.java`

メイドさんが死亡した際に生成される魂エンティティ：

### 基本特性
```java
public class MaidSoulEntity extends Entity {
    private MaidSoul maidSoul;  // 死亡時のメイドさんデータ
    
    @Override
    public void initDataTracker() {
        this.noClip = true;      // ブロック貫通
        // 重量21g（コメント）
    }
}
```

### 魂データ (MaidSoul)
死亡したメイドさんの全データを保持：

```java
public class MaidSoul {
    // 基本情報
    private String maidName;
    private UUID ownerId;
    private float health;
    private int experience;
    
    // インベントリデータ
    private NbtCompound inventoryData;
    
    // 設定データ
    private NbtCompound settingData;
    
    // 外見データ
    private NbtCompound modelData;
}
```

### 復活システム
```java
public boolean resurrectionMaid(World world, BlockPos pos, PlayerEntity player) {
    if (canResurrection(player)) {
        LittleMaidEntity maid = LMRBEntities.createLittleMaid(world);
        maidSoul.setDataToMaid(maid);  // データ復元
        world.spawnEntity(maid);
        return true;
    }
    return false;
}
```

---

## 🎮 GUI システム

### LittleMaidScreenHandler
**場所**: `/entity/LittleMaidScreenHandler.java`

メイドさんのインベントリGUIを管理：

#### スロット配置
```java
public LittleMaidScreenHandler(int syncId, PlayerInventory playerInventory, LittleMaidEntity maid) {
    // メイドインベントリ（0-17）
    for (int i = 0; i < 18; i++) {
        this.addSlot(new Slot(maid.getInventory(), i, x, y));
    }
    
    // メイン・オフハンドスロット（18-19）
    this.addSlot(new Slot(maid.getHandItems(), 0, x, y));
    this.addSlot(new Slot(maid.getHandItems(), 1, x, y));
    
    // 防具スロット（20-23）
    for (int i = 0; i < 4; i++) {
        this.addSlot(new ArmorSlot(maid.getArmorItems(), i, x, y));
    }
    
    // プレイヤーインベントリ（24-59）
    // ...
}
```

#### アイテム移動システム
```java
@Override
public ItemStack quickMove(PlayerEntity player, int index) {
    // インデックスに応じた適切なスロット移動処理
    if (index < 18) {
        // メイドインベントリ → プレイヤーインベントリ
    } else if (index < 24) {
        // 装備スロット → 適切な場所
    }
    // ...
}
```

---

## 🔄 初期化と設定

### AI行動ゴールの初期化

`initGoals()` メソッドで行動ゴールを優先度順に登録：

```java
@Override
protected void initGoals() {
    // 緊急時行動（最高優先度）
    this.goalSelector.add(0, new LMEmergencyTeleportGoal(this));
    this.goalSelector.add(1, new SwimGoal(this));
    this.goalSelector.add(2, new LongDoorInteractGoal(this, true));
    
    // 生存行動
    this.goalSelector.add(3, new LMHealMyselfGoal(this));
    this.goalSelector.add(4, new LMCollectSalaryFromContainerGoal(this));
    this.goalSelector.add(5, new WaitGoal(this));
    
    // 移動・戦闘行動
    this.goalSelector.add(6, new LMTeleportTameOwnerGoal(this));
    this.goalSelector.add(7, new FleeEntityGoal<>(this, /*...*/));
    this.goalSelector.add(8, new ModeWrapperGoal(this));
    this.goalSelector.add(9, new HasMMFollowTameOwnerGoal(this, /*...*/));
    
    // 日常行動（低優先度）
    this.goalSelector.add(10, new FollowAtHeldItemGoal(this, /*...*/));
    this.goalSelector.add(11, new LMStoreItemToContainerGoal(this));
    this.goalSelector.add(12, new LMMoveToDropItemGoal(this));
    this.goalSelector.add(13, new PlaySnowGoal(this));
    this.goalSelector.add(14, new RedstoneTraceGoal(this));
    this.goalSelector.add(15, new WanderAroundFarGoal(this, /*...*/));
    
    // ターゲティング
    this.targetSelector.add(0, new LMTargetGoal(this));
}
```

---

## 📈 パフォーマンス最適化

### 効率的な状態管理
- **ビットフラグ**: 複数のboolean状態を単一のintで管理
- **遅延初期化**: 必要時のみオブジェクトを生成
- **キャッシュ**: 計算結果の適切な保存

### メモリ使用量最適化
- **Optional型**: null参照の安全な処理
- **Weak参照**: 循環参照の回避
- **適切な破棄**: 不要オブジェクトの適切な解放

---

## 🔧 拡張性

### 新機能の追加方法

#### 1. 新しいインターフェースの実装
```java
public interface HasNewFeature {
    void doNewFeature();
    boolean canUseNewFeature();
}
```

#### 2. 委譲クラスの作成
```java
public class NewFeatureImpl implements HasNewFeature {
    // 機能の具体的な実装
}
```

#### 3. LittleMaidEntityへの統合
```java
public class LittleMaidEntity extends TameableEntity implements /*...*/, HasNewFeature {
    private final NewFeatureImpl newFeature = new NewFeatureImpl();
    
    @Override
    public void doNewFeature() {
        newFeature.doNewFeature();  // 委譲
    }
}
```

---

## 📋 まとめ

エンティティシステムは以下の特徴を持つ優秀な設計です：

### ✅ 優秀な点
- **関心の分離**: 各機能が独立したクラスで実装
- **委譲パターン**: 適切な責任分散
- **データ整合性**: DataTrackerとNBTによる確実な同期・永続化
- **拡張性**: 新機能の追加が容易
- **型安全性**: インターフェースによる契約の明確化

### 🚀 今後の発展可能性
- **新しい機能インターフェース**の追加
- **AI学習システム**の統合
- **より高度な状態管理**システム
- **外部MOD連携**インターフェース

このエンティティシステムは、複雑な機能を持つエンティティの実装における優秀なアーキテクチャ例として、他のプロジェクトでも参考にできる設計となっています。