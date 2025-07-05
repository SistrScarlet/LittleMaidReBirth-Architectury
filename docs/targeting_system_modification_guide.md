# TargetingSystem変更手順ガイド

このドキュメントでは、TargetingSystemに新しい機能や修正を追加する際の標準的な手順を説明します。

## 概要

TargetingSystemは、メイドさんの戦闘ターゲット選択を管理するシステムです。新しい優先度修正や判定条件を追加する場合は、以下の手順に従ってください。

## 変更手順

### 1. TargetingSystem.javaの変更

#### 1.1 新しいModifierメソッドの追加
新しい優先度修正ロジックを実装するpublicメソッドを追加します。

```java
/**
 * [機能の説明]
 */
public static double calculate[機能名]Modifier(Mob mob, Maid maid, @Nullable Master master, List<Maid> otherMaids) {
    // 実装内容
    return modifierValue;
}
```

#### 1.2 優先度計算への組み込み
`calculateEnemyPriorities`メソッド内の優先度計算部分に新しいmodifierを追加します。

```java
double score = basePriority;
score += calculateDistanceModifier(mob, maid, master);
score += calculateWeaponCompatibility(mob, maid);
score += calculateDangerModifier(mob, maid, master, otherMaids);
score += calculateDistributionModifier(mob, otherMaids);
score += calculateMasterStanceModifier(mob, master, combatSettings);
score += calculate[新機能名]Modifier(mob, maid, master, otherMaids); // 新規追加
```

### 2. TargetingConfig.javaの変更

新しい設定値の取得メソッドを追加します。

```java
/**
 * [設定値の説明]
 */
public static double get[設定名]() {
    return LMRBMod.getConfig().advancedTarget.[設定名];
}
```

### 3. LMRBConfig.javaの変更

#### 3.1 AdvancedTargetクラスへの追加
新しいコンフィグ項目をAdvancedTargetクラスに追加します。

```java
@ConfigEntry.Gui.Tooltip
public double [設定名] = [デフォルト値];
```

### 4. 言語ファイルの更新

#### 4.1 en_us.jsonの更新
```json
"text.autoconfig.littlemaidrebirth.option.advancedTarget.[設定名]": "[英語での設定名]",
"text.autoconfig.littlemaidrebirth.option.advancedTarget.[設定名].@Tooltip": "[英語での説明文]",
```

#### 4.2 ja_jp.jsonの更新
```json
"text.autoconfig.littlemaidrebirth.option.advancedTarget.[設定名]": "[日本語での設定名]",
"text.autoconfig.littlemaidrebirth.option.advancedTarget.[設定名].@Tooltip": "[日本語での説明文]",
```

## 実装例: 負傷身内攻撃者ボーナス

今回実装した「負傷している身内の攻撃者に対する優先度ボーナス」を例に、具体的な実装を示します。

### 1. TargetingSystem.java
```java
/**
 * 負傷している身内の攻撃者に対する優先度ボーナス
 */
public static double calculateInjuredAllyAttackerModifier(Mob mob, Maid maid, @Nullable Master master, List<Maid> otherMaids) {
    double injuredAllyAttackerBonus = 0;
    
    // ご主人が負傷している場合
    if (master != null && master.isInjured() && master.isAttackedBy(mob)) {
        injuredAllyAttackerBonus += TargetingConfig.getInjuredMasterAttackerBonus();
    }
    
    // 他のメイドさんが負傷している場合
    for (Maid otherMaid : otherMaids) {
        if (otherMaid.isInjured() && otherMaid.isAttackedBy(mob)) {
            injuredAllyAttackerBonus += TargetingConfig.getInjuredMaidAttackerBonus();
        }
    }
    
    return injuredAllyAttackerBonus;
}
```

### 2. TargetingConfig.java
```java
/**
 * 負傷したご主人の攻撃者への優先度ボーナス
 */
public static double getInjuredMasterAttackerBonus() {
    return LMRBMod.getConfig().advancedTarget.injuredMasterAttackerBonus;
}

/**
 * 負傷したメイドさんの攻撃者への優先度ボーナス
 */
public static double getInjuredMaidAttackerBonus() {
    return LMRBMod.getConfig().advancedTarget.injuredMaidAttackerBonus;
}
```

### 3. LMRBConfig.java
```java
@ConfigEntry.Gui.Tooltip
public double injuredMasterAttackerBonus = 150.0;
@ConfigEntry.Gui.Tooltip
public double injuredMaidAttackerBonus = 100.0;
```

## 注意事項

1. **メソッド名の統一**: calculateXxxModifierの形式に統一する
2. **publicメソッド**: TargetingSystemのメソッドはpublicにして、テストやデバッグで呼び出し可能にする
3. **@Nullableアノテーション**: masterパラメータは常に@Nullableを付ける
4. **設定値の管理**: 新しい数値はハードコードせず、必ずTargetingConfigを経由してLMRBConfigから取得する
5. **言語ファイル**: 両方の言語ファイル（en_us.json、ja_jp.json）を必ず更新する
6. **デフォルト値**: バランスを考慮した適切なデフォルト値を設定する

## テスト手順

1. 設定値の変更がゲーム内のコンフィグGUIに正しく反映されることを確認
2. 新機能が期待通りに動作することを確認
3. 既存の機能に影響がないことを確認
4. 言語切り替えで正しく翻訳されることを確認

## 関連ファイル

- `TargetingSystem.java`: 主要なロジック
- `TargetingConfig.java`: 設定値アクセス
- `LMRBConfig.java`: 設定値定義
- `en_us.json`: 英語翻訳
- `ja_jp.json`: 日本語翻訳

このガイドに従うことで、TargetingSystemの一貫性を保ちながら新機能を追加できます。