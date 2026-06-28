# 武器 Mod 連携 API の設計調査

## 背景

LMRBCompat で各武器 Mod との互換性を提供しているが、現状は武器 Mod の内部実装に直接依存している。
武器 Mod 作者が自力で LMRB 対応できるよう、LMRB 側の API を拡充する方針を検討する。

## 現状の課題

LMRBCompat の各 ShooterMode は以下のような内部実装依存を持つ:

- `weapon.pellet`, `weapon.bure`, `weapon.fire_posx` 等のフィールド直参照
- 弾生成ロジック（`EntityB_Bullet`, `CGEntityBullet` 等）のコピー実装
- 音声再生の自前実装（`weapon.fire_sound` を読んで `world.playSound()`）
- マズルフラッシュ・パーティクルの座標計算コピー

武器 Mod が API 変更すると即座に壊れる構造であり、LMRBCompat がメンテナンスコストを負い続ける。

## 現状の LMRB API

```
net.sistr.littlemaidrebirth.api.mode/
├── Mode                 # AI モードの基底クラス
├── ModeType             # モード生成 + アイテムマッチング
├── ModeManager          # モード登録レジストリ
├── ItemMatcher          # アイテム判定インターフェース
├── ItemMatchers         # マッチャーファクトリ (clazz, item, tag, name)
├── IRangedWeapon        # 射程・発射レートを提供するインターフェース
└── Modes                # デフォルトモード定義
```

`IRangedWeapon` を実装すればデフォルトの ArcherMode で「使う」ことはできるが、
リロード・弾種切替・マズルフラッシュ等の銃固有の挙動は実現できない。

## 提案: 追加すべき API

### 1. IMaidUsableRangedWeapon — 射撃アクション API

`IRangedWeapon` の拡張。武器 Mod が実装することで、弾生成・音・エフェクトを武器側で完結できる。

```java
public interface IMaidUsableRangedWeapon extends IRangedWeapon {
    // メイドさんが射撃を実行する（弾生成・音・エフェクト全部込み）
    void fireByMaid(LittleMaidEntity maid, LivingEntity target, ItemStack stack);

    // リロード関連
    boolean needsReload(ItemStack stack);
    int getReloadTicks(ItemStack stack);
    void startReload(LittleMaidEntity maid, ItemStack stack);
    void finishReload(LittleMaidEntity maid, ItemStack stack);

    // 弾があるか（メイドのインベントリ検索用）
    boolean hasAmmo(LittleMaidEntity maid, ItemStack weapon);

    // フルオートか
    boolean isFullAuto(ItemStack stack);
}
```

LMRB 側にこの API 対応の ShooterMode を用意すれば、武器 Mod 作者は
自分の弾生成ロジックをそのまま使え、LMRBCompat のような内部実装コピーが不要になる。

```java
// LMRB 側の汎用 ShooterMode（API 対応武器用）
public class ShooterMode extends AbstractArcherMode<IMaidUsableRangedWeapon> {
    @Override
    protected void tickRangedAttack(LivingEntity target, ItemStack stack, ...) {
        if (weapon.needsReload(stack)) {
            weapon.startReload(maid, stack);
            return;
        }
        weapon.fireByMaid(maid, target, stack);
    }
}
```

### 2. IMaidUsableMeleeWeapon — 近接アクション API

SlashBlade のようなコンボ系武器用。

```java
public interface IMaidUsableMeleeWeapon {
    void attackByMaid(LittleMaidEntity maid, LivingEntity target, ItemStack stack);
    int getAttackCooldown(ItemStack stack);
    float getAttackRange(ItemStack stack);
}
```

### 3. レンダラー登録イベント

現状 LMRBCompat は Mixin で MaidModelRenderer に FeatureRenderer を注入している。
LMRB がイベントを提供すれば Mixin なしで武器描画を追加できる。

```java
public class MaidRenderEvents {
    public static final Event<AddFeature> ADD_FEATURE =
        EventFactory.createArrayBacked(AddFeature.class, ...);

    @FunctionalInterface
    public interface AddFeature {
        void addFeature(MaidModelRenderer renderer, EntityRendererFactory.Context ctx);
    }
}
```

### 4. tick フックイベント

SlashBlade 互換で `inventoryTick` を手動呼び出ししている問題の解消用。

```java
public class MaidTickEvents {
    public static final Event<MaidTick> TICK =
        EventFactory.createArrayBacked(MaidTick.class, ...);

    @FunctionalInterface
    public interface MaidTick {
        void onTick(LittleMaidEntity maid);
    }
}
```

## API 追加後の世界観

| レイヤー | 現状（LMRBCompat が担当） | API 追加後（武器 Mod 作者が自力対応可） |
|---------|-------------------------|---------------------------------------|
| モード登録 | `ModeType` + `ItemMatcher` | そのまま（十分） |
| 射撃実行 | 内部実装をコピー | `IMaidUsableRangedWeapon.fireByMaid()` |
| 近接実行 | 内部実装をコピー | `IMaidUsableMeleeWeapon.attackByMaid()` |
| レンダラー | Mixin で注入 | イベントで登録 |
| tick フック | Mixin で inject | イベントで登録 |

LMRBCompat の存在意義は「API に対応してくれない Mod のための互換レイヤー」に限定される。

## 実装の優先順位案

| 順位 | 項目 | 効果 |
|------|------|------|
| 1 | `IMaidUsableRangedWeapon` | 銃 Mod 作者の自力対応を可能にする（最も需要が高い） |
| 2 | レンダラー登録イベント | Mixin 不要化、武器描画の外部対応を容易にする |
| 3 | `IMaidUsableMeleeWeapon` | 近接武器 Mod の自力対応（SlashBlade 等） |
| 4 | tick フックイベント | 特殊な要件向け（優先度低） |

## 結論

LMRB 側に `IMaidUsableRangedWeapon` とレンダラー登録イベントを追加すれば、
武器 Mod 作者は LMRB の jar を `compileOnly` で依存するだけで自力対応できるようになる。
具体的な実装は LMRB リポジトリ側で行う。
