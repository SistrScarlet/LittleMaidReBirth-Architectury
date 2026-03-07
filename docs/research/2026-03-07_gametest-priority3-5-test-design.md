# 第3〜5優先グループ テスト設計

## 第3優先: 重要度中 × テスト容易〜中程度

### 1. Freedom / Tracer 切替

#### テスト対象

- `LMInteractionHandler.handleFeather()` — ESCORT↔FREEDOM
- `LMInteractionHandler.handleRedstone()` — FREEDOM↔TRACER

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| MOV-1 | 羽で ESCORT→FREEDOM | テイム済み + ESCORT | 羽を持って interactMob | movingMode=FREEDOM, freedomPos 設定 |
| MOV-2 | 羽で FREEDOM→ESCORT | テイム済み + FREEDOM | 羽を持って interactMob | movingMode=ESCORT |
| MOV-3 | レッドストーンで FREEDOM→TRACER | テイム済み + FREEDOM | レッドストーンを持って interactMob | movingMode=TRACER |
| MOV-4 | レッドストーンで TRACER→FREEDOM | テイム済み + TRACER | レッドストーンを持って interactMob | movingMode=FREEDOM |
| MOV-5 | ESCORT 時はレッドストーン無効 | テイム済み + ESCORT | レッドストーンを持って interactMob | movingMode 変化なし（GUI が開く） |

---

### 2. Ripper モード

#### テスト対象

- `RipperMode` — ハサミでモード判定

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| RIP-1 | ハサミで Ripper モード | テイム済み + ハサミ装備 | hasModeImpl.tick() | getMode() = "Ripper" |
| RIP-2 | ハサミ以外では Ripper にならない | テイム済み + 石装備 | hasModeImpl.tick() | getMode() != "Ripper" |

---

### 3. Healer モード

#### テスト対象

- `HealerMode` — 食べ物 / ポーションでモード判定

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| HEAL-1 | 食べ物で Healer モード | テイム済み + パン装備 | hasModeImpl.tick() | getMode() = "Healer" |
| HEAL-2 | ポーションで Healer モード | テイム済み + 治癒ポーション装備 | hasModeImpl.tick() | getMode() = "Healer" |
| HEAL-3 | 武器では Healer にならない | テイム済み + 剣装備 | hasModeImpl.tick() | getMode() != "Healer" |

---

### 4. ドロップアイテム拾い

#### テスト対象

- `LittleMaidEntity.pickupItem()` — 近くのアイテムを拾う
- `LMMoveToDropItemGoal` — アイテムに向かって移動

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| PICK-1 | 近くのアイテムを拾う | テイム済み + 近くに ItemEntity | tick 待ち | メイドさんのインベントリにアイテムが入る |
| PICK-2 | 野良は拾わない（デフォルト） | 野良 + 近くに ItemEntity | tick 待ち | インベントリにアイテムが入らない |

#### 補足

- `addInstantFinalTask` で毎 tick チェック、`tickLimit = 200`
- アイテムはメイドさんの足元にスポーンして距離0で拾わせるのが確実

---

### 5. アイテム格納（チェスト）

#### テスト対象

- `LMStoreItemToContainerGoal` — チェストへのアイテム移動

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| STORE-1 | 不要アイテムをチェストに格納 | テイム済み + インベントリにアイテム + 近くにチェスト | tick 待ち | チェストにアイテムが入る |

#### 補足

- ストラクチャーでチェストを配置するか、`context.setBlockState()` で配置
- Goal の動作は tick 依存（パス探索→移動→格納）なので `tickLimit` 延長が必要
- テスト困難度が高いため、最初は保留でも可

---

### 6. お給料消費・ストライキ

#### テスト対象

- `LMItemContractable.tick()` — 給料消費と未払い日数管理
- ストライキ発動条件

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| SAL-1 | 給料があれば消費される | テイム済み + インベントリに砂糖 | consumeSalaryInterval tick 経過 | 砂糖が1個減少 |
| SAL-2 | 給料がないと未払い日数が増加 | テイム済み + インベントリに砂糖なし | consumeSalaryInterval tick 経過 | unpaidDays 増加 |
| SAL-3 | 未払い日数超過でストライキ | テイム済み + unpaidDays = unpaidDaysLimit | tick | isStrike=true |

#### 補足

- `consumeSalaryInterval`（デフォルト 24000 tick）は長すぎるので、コンフィグを一時変更して短くするか、`itemContractable` の内部状態を直接操作
- テスト困難度が中程度

---

## 第4優先: 作業モード（ストラクチャー必要）

### 7. Cooking（かまど）

#### テスト対象

- `BlockWorkMode` + `FurnaceWorkStrategy` — かまど探索→材料投入→精錬→回収

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| COOK-1 | Cooking モード判定 | テイム済み + Cooking タグアイテム装備 | hasModeImpl.tick() | getMode() = "Cooking" |
| COOK-2 | かまどに材料を投入 | Cooking モード + 近くにかまど + インベントリに材料 | tick 待ち | かまどに材料が入る |
| COOK-3 | 精錬完了品を回収 | Cooking モード + かまどに精錬完了品 | tick 待ち | メイドさんのインベントリに精錬品 |

#### 補足

- かまど付きストラクチャーが必要（nbt.py で生成）
- 精錬には時間がかかるため `tickLimit` を大きく延長する必要がある
- `BlockReservationManager` による排他制御もテスト対象候補

---

### 8. Pharmacist（醸造台）

#### テスト対象

- `BlockWorkMode` + `BrewingWorkStrategy`

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| PHARM-1 | Pharmacist モード判定 | テイム済み + 水入り瓶装備 | hasModeImpl.tick() | getMode() = "Pharmacist" |
| PHARM-2 | 醸造台に材料を投入 | Pharmacist モード + 近くに醸造台 | tick 待ち | 醸造台に材料が入る |

#### 補足

- 醸造台付きストラクチャーが必要
- Cooking と同パターン

---

### 9. Torcher（松明設置）

#### テスト対象

- `TorcherMode` — 暗所検知 + 松明設置

#### テストケース

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| TORCH-1 | Torcher モード判定 | テイム済み + 松明装備 | hasModeImpl.tick() | getMode() = "Torcher" |
| TORCH-2 | 暗所に松明を設置 | Torcher モード + 暗い場所 | tick 待ち | 松明ブロックが設置される |

#### 補足

- 暗所ストラクチャー（天井で光を遮断）が必要
- `torcherLightLevelThreshold`（デフォルト 7）以下の場所に設置

---

## 第5優先: テスト困難 or 低重要度

### 10. Archer（射撃）

| ID | テスト名 | 前提条件 | 操作 | 検証項目 |
|----|---------|---------|------|---------|
| ARCH-1 | Archer モード判定 | テイム済み + 弓装備 + 矢所持 | hasModeImpl.tick() | getMode() = "Archer" |
| ARCH-2 | 射撃でターゲットにダメージ | Archer モード + ターゲット | tick 待ち（射撃 AI 動作） | ターゲットの HP 減少 |

#### 補足

- 射撃は弾道計算+射線チェックがあるため、ターゲットの配置が重要
- クロスボウのチャージ→射撃サイクルもテスト候補
- テスト困難度: 高

### 11. インタラクション各種（低優先）

| ID | テスト名 | 操作 | 検証項目 |
|----|---------|------|---------|
| INT-1 | サドルで肩車 | サドルを持って interactMob | maid.getVehicle() == player |
| INT-2 | ガラス瓶→エンチャ瓶 | 経験値あり + ガラス瓶 | エンチャ瓶に変換、経験値減少 |
| INT-3 | 火薬で加速 | 火薬を持って interactMob | accelerationTicks > 0 |
| INT-4 | バケツでミルク | canMilking=true + バケツ | ミルクバケツに変換 |

### 12. その他

| ID | テスト名 | 操作 | 検証項目 |
|----|---------|------|---------|
| MISC-1 | 自然スポーン条件 | isValidNaturalSpawn(world, pos) | 明るさ8超 + 固体ブロック上 |
| MISC-2 | 死亡時インベントリドロップ | テイム済み + インベントリにアイテム + kill() | ドロップアイテムが存在 |
| MISC-3 | 経験値オーブ拾い | メイドさんの近くに経験値オーブ | experiencePoints 増加 |

---

## ケース数まとめ

| 優先度 | カテゴリ | ケース数 | 実装状況 |
|--------|---------|---------|---------|
| 第3 | Freedom/Tracer 切替 | 5 | 未実装 |
| 第3 | Ripper モード | 2 | 未実装 |
| 第3 | Healer モード | 3 | 未実装 |
| 第3 | ドロップアイテム拾い | 2 | 未実装 |
| 第3 | アイテム格納 | 1 | 未実装 |
| 第3 | お給料消費・ストライキ | 3 | 未実装 |
| 第4 | Cooking | 3 | 未実装 |
| 第4 | Pharmacist | 2 | 未実装 |
| 第4 | Torcher | 2 | 未実装 |
| 第5 | Archer | 2 | 未実装 |
| 第5 | インタラクション各種 | 4 | 未実装 |
| 第5 | その他 | 3 | 未実装 |
| | **合計** | **32** | |
