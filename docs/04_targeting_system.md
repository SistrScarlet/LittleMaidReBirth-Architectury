# ターゲティングシステム詳細仕様（TargetTagシステム）

## 📋 概要

Little Maid Rebirth のターゲティングシステムは、メイドさんの戦闘判断を司るシンプルで効率的なAIシステムです。従来のIFFシステムから、より細かい制御が可能なTargetTagシステムに完全移行しました。プレイヤーがエンティティごとに詳細なターゲティング設定を行えるようになっています。

---

## 🏗️ システム構成

### TargetTagアーキテクチャ

```
ターゲティングシステム
├── TargetingSystem.java          - 3段階優先度計算エンジン
│   ├── selectTarget()           - メイン選択ロジック
│   ├── determinePriority()      - 優先度判定
│   ├── TargetPriority enum      - 3段階優先度定義
│   └── TargetTag enum           - 5種類のターゲットタグ
├── LMTargetGoal.java            - AI行動への統合
├── TargetingConfig.java         - シンプル設定管理
├── TargetTagシステム             - エンティティごとの細かい制御
│   ├── TargetTagManager.java    - タグ管理インターフェース
│   ├── TargetTagManagerImpl.java - 実装クラス
│   └── TargetIdentifier.java    - エンティティ識別子
├── UIシステム                    - プレイヤー設定画面
│   └── TargetTagScreen.java     - フィルタリング可能な設定画面
└── ネットワーク                  - サーバー同期
    └── C2SSetTargetTagsPacket.java - タグ情報同期
```

---

## 🎯 3段階優先度システム

### シンプル優先度判定

ターゲティングシステムは以下の3段階で優先度を判定します：

```java
public enum TargetPriority {
    CRITICAL,  // 自分の身を守る
    HIGH,      // ご主人の身を守る  
    NORMAL     // 味方の身を守る・周囲の対象
}
```

---

## 🏷️ TargetTagシステム

### 5種類のTargetTag

**場所**: `TargetingSystem.java:34-40`

```java
public enum TargetTag {
    APPROACH_PROHIBITED,         // 接近禁止
    ATTACK_PROHIBITED,           // 攻撃禁止
    PREEMPTIVE_ATTACK_PROHIBITED, // 先制攻撃禁止（反撃は可能）
    MELEE_WEAPON_PROHIBITED,     // 近距離攻撃禁止
    RANGED_WEAPON_PROHIBITED    // 遠距離攻撃禁止
}
```

### TargetTag詳細説明

1. **APPROACH_PROHIBITED** - 接近禁止
   - 対象：Creeper、TNTなど爆発系エンティティ
   - 効果：一定距離を保って遠距離攻撃のみ実行

2. **ATTACK_PROHIBITED** - 攻撃禁止
   - 対象：Wither、Ender Dragon、Wardenなど危険エンティティ
   - 効果：一切攻撃せず、近づいたら避難行動を取る

3. **PREEMPTIVE_ATTACK_PROHIBITED** - 先制攻撃禁止
   - 対象：友好的だが反撃する可能性があるエンティティ
   - 効果：攻撃を受けた場合のみ反撃可能

4. **MELEE_WEAPON_PROHIBITED** - 近距離攻撃禁止
   - 対象：WitherSkeleton、Ravagerなど強力な近接攻撃持ち
   - 効果：弓やクロスボウによる遠距離攻撃のみ

5. **RANGED_WEAPON_PROHIBITED** - 遠距離攻撃禁止
   - 対象：Endermanなど投射物で挑発されるエンティティ
   - 効果：剣などの近接武器による攻撃のみ

---

## 💾 TargetTagManager

### エンティティ別タグ管理

**場所**: `TargetTagManagerImpl.java`

```java
public interface TargetTagManager {
    Set<TargetingSystem.TargetTag> getTargetTags(TargetIdentifier identifier);
    void setTargetTags(TargetIdentifier identifier, Set<TargetingSystem.TargetTag> tags);
    Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> getAllTargetTags();
}
```

### TargetIdentifier

エンティティタイプとオプションの識別情報でターゲットを一意に識別：

```java
public class TargetIdentifier {
    private final EntityType<?> entityType;
    private final Optional<String> identifier;
    
    // エンティティタイプ + 特定の識別子（必要に応じて）
}
```

---

## 🖥️ TargetTagScreen - プレイヤー設定UI

### フィルタリング可能な設定画面

**場所**: `TargetTagScreen.java`

新しいTargetTagScreenは、FilterableListGUIを使用してプレイヤーが直感的にエンティティごとのターゲットタグを設定できます：

```java
public class TargetTagScreen extends Screen {
    private final Entity entity;
    private final Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags;
    private FilterableListGUI<TargetTagGUIElement> targetTagGui;
}
```

### 主要機能

1. **リアルタイム検索**: エンティティ名で即座にフィルタリング
2. **直感的な設定**: アイコンボタンでタグのオン/オフ切り替え
3. **状態可視化**: 各ターゲットタグの現在の設定を一目で確認
4. **自動同期**: 画面を閉じると自動的にサーバーと同期

### GUIレイアウト

```
TargetTagScreen
├── 検索入力欄 (上部)
├── エンティティリスト (中央)
│   ├── エンティティ名表示
│   ├── 攻撃制御ボタン (禁止/制限/許可)
│   ├── 武器制御ボタン (近接/遠距離制限)
│   └── 接近制御ボタン (許可/禁止)
└── スクロールバー (右端)
```

---

## 📡 ネットワーク同期

### C2SSetTargetTagsPacket

**場所**: `C2SSetTargetTagsPacket.java`

プレイヤーの設定をサーバーに送信：

```java
public class C2SSetTargetTagsPacket {
    public static <T extends Entity & TargetTagManager> void sendC2SPacket(
        T entity, Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> targetTags) {
        // パケット送信処理
    }
}
```

### 同期タイミング

1. **TargetTagScreen終了時**: 設定変更を自動送信
2. **ログイン時**: サーバーから現在の設定を受信
3. **設定変更時**: リアルタイムでサーバーと同期

---

## ⚙️ TargetingConfig - 設定システム

### 設定項目一覧

**場所**: `TargetingConfig.java` / `LMRBConfig.java:Target`

```java
public static class Target {
    // 距離関連設定
    public int alertRange = 16;              // 警戒範囲（敵検出・先制攻撃範囲）
    public int combatRange = 8;              // 戦闘範囲（実際の戦闘行動範囲）
    public int dangerousAvoidDistance = 8;   // 危険敵回避距離
    
    // 分散ターゲティング設定
    public double distributionRatio = 0.5;   // 分散比率（メイドさん数の50%）
    public int maxAttackersPerEnemy = 2;     // 1体あたり最大攻撃者数
    
    // 体力関連設定
    public float injuredThreshold = 0.5f;    // 負傷判定閾値（体力50%以下）
    public int attackedByValidTicks = 100;   // 攻撃判定有効時間（5秒間）
}
```

---

## 📈 TargetTagシステムによる改善効果

### IFFシステムからの進歩

**移行前（IFFシステム）**:
- 3段階の固定的な敵味方識別（FRIEND/ENEMY/UNKNOWN）
- エンティティタイプ単位での粗い制御
- プレイヤーが設定変更できない固定ルール

**移行後（TargetTagシステム）**:
- 5種類の柔軟なターゲットタグで細かい制御
- エンティティごとの個別設定が可能
- プレイヤーがリアルタイムで設定変更可能

### プレイヤー体験の向上

1. **細かい制御**: エンティティごとに攻撃・接近・武器使用を個別設定
2. **直感的UI**: TargetTagScreenで視覚的にタグを設定
3. **検索機能**: 大量のエンティティから素早く対象を見つけられる
4. **状態可視化**: 現在の設定を一目で確認可能

### 開発効率の向上

1. **拡張性**: 新しいTargetTagを簡単に追加可能
2. **保守性**: 設定とロジックの分離でバグを減少
3. **テスト性**: タグベースの明確な動作仕様
4. **再利用性**: 他のMODでも応用可能な汎用設計

---

## 📋 まとめ

### ✅ TargetTagシステムの実装済み機能

- **5種類のTargetTag**: 攻撃禁止、接近禁止、先制攻撃禁止、近接攻撃禁止、遠距離攻撃禁止
- **TargetTagManager**: エンティティごとのタグ管理システム
- **TargetTagScreen**: フィルタリング可能な設定画面
- **リアルタイム同期**: ネットワークパケットによる即座の設定反映
- **3段階優先度システム**: CRITICAL > HIGH > NORMAL（従来から継承）

### 🎯 システム設計の優秀性

- **柔軟性**: エンティティごとの個別制御
- **直感性**: プレイヤーが理解しやすいタグシステム
- **拡張性**: 新しいタグやルールを簡単に追加
- **パフォーマンス**: 効率的なタグベース判定
- **保守性**: 設定とロジックの明確な分離

### 🚀 技術的革新

- **FilterableListGUI統合**: 検索・フィルタリング機能付きUI
- **Builder パターン**: 宣言的で読みやすい設定記述
- **状態復元システム**: 画面開閉時の設定状態保持
- **型安全性**: 強い型付けによるバグ防止

**TargetTagシステムは、プレイヤーの戦術的自由度を大幅に向上させ、MODの拡張性と保守性を両立した、次世代のターゲティングシステムです。**