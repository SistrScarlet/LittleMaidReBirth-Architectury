# メイドさん統合ターゲティングシステム - アルゴリズム設計

## Phase0: 設計概要

このドキュメントでは、target.mdで定義された17のシナリオを満たすターゲティングアルゴリズムを設計します。

### 設計目標

1. **テスト駆動設計**: 各シナリオを数値的に検証可能なテストコードに変換
2. **優先度ベース算出**: メイドさんから見た各敵の優先度を数値スコアで算出
3. **拡張可能性**: 新しいシナリオや敵タイプに対応可能な設計

### 設計の流れ

- **Phase1**: シナリオのテストコード化
- **Phase2**: 優先度算出アルゴリズムの設計
- **Phase3**: 実装に向けた追加設計作業

### 前提条件

- メイドさんの武器種別（近距離/遠距離）を判定可能
- 敵の種別・位置・行動状態を取得可能
- ご主人の位置・ターゲットを取得可能
- 他メイドさんの状態・ターゲットを取得可能

---

## Phase1: シナリオのテストコード化

各シナリオを数値的に検証可能なテストケースに変換します。

### テストフレームワーク設計

```python
class TargetingTestCase:
    def __init__(self):
        self.world = World()
        self.maids = []
        self.enemies = []
        self.master = None
    
    def add_maid(self, name, weapon_type, position, health=20):
        maid = Maid(name, weapon_type, position, health)
        self.maids.append(maid)
        return maid
    
    def add_enemy(self, name, enemy_type, position, is_dangerous=False):
        mob = Enemy(name, enemy_type, position, is_dangerous)
        self.enemies.append(mob)
        return mob
    
    def set_master(self, position):
        self.master = Master(position)
    
    def set_attack_action(self, attacker, target):
        attacker.set_target(target)
    
    def calculate_priorities(self, maid):
        # Phase2で実装するアルゴリズムを呼び出し
        return targeting_algorithm.calculate_enemy_priorities(maid, self.enemies, self.master, self.maids)
    
    def assert_target_priority(self, maid, expected_target, message=""):
        priorities = self.calculate_priorities(maid)
        actual_target = max(priorities, key=priorities.get) if priorities else None
        assert actual_target == expected_target, f"{message}: Expected {expected_target}, got {actual_target}"
```

### シナリオ1: 基本的な攻撃反応

```python
def test_scenario1_basic_attack_response():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "sword", Position(1, 1))
    zombie = test.add_enemy("Zombie", "zombie", Position(0, 1))
    
    # ゾンビがご主人を攻撃
    test.set_attack_action(zombie, master)
    
    # 検証: メイドさんは即座にゾンビをターゲット
    test.assert_target_priority(maid_a, zombie, "基本的な攻撃反応")
```

### シナリオ2: 複数敵の優先度判定

```python
def test_scenario2_multiple_enemy_priority():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "sword", Position(1, 1))
    skeleton = test.add_enemy("Skeleton", "skeleton", Position(5, 0))  # 5ブロック先
    zombie = test.add_enemy("Zombie", "zombie", Position(2, 0))        # 2ブロック先
    
    # 検証: より近いゾンビを優先
    test.assert_target_priority(maid_a, zombie, "距離による優先度判定")
```

### シナリオ3: 攻撃者への優先対応

```python
def test_scenario3_attacker_priority():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "sword", Position(1, 1))
    skeleton = test.add_enemy("Skeleton", "skeleton", Position(5, 0))  # 遠い
    zombie = test.add_enemy("Zombie", "zombie", Position(2, 0))        # 近い
    
    # スケルトンがご主人を攻撃
    test.set_attack_action(skeleton, master)
    
    # 検証: 距離は遠いが攻撃者のスケルトンを優先
    test.assert_target_priority(maid_a, skeleton, "攻撃者優先対応")
```

### シナリオ4: 複数メイドさんの分散攻撃

```python
def test_scenario4_distributed_attack():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "sword", Position(1, 1))
    maid_b = test.add_maid("MaidB", "sword", Position(-1, 1))
    maid_c = test.add_maid("MaidC", "sword", Position(0, 2))
    zombie1 = test.add_enemy("Zombie1", "zombie", Position(1, 0))
    zombie2 = test.add_enemy("Zombie2", "zombie", Position(-1, 0))
    
    # 検証: 異なるゾンビをターゲット
    priorities_a = test.calculate_priorities(maid_a)
    priorities_b = test.calculate_priorities(maid_b)
    priorities_c = test.calculate_priorities(maid_c)
    
    target_a = max(priorities_a, key=priorities_a.get)
    target_b = max(priorities_b, key=priorities_b.get)
    target_c = max(priorities_c, key=priorities_c.get)
    
    # 3体のメイドさんが同じ敵を狙わないことを検証
    targets = [target_a, target_b, target_c]
    unique_targets = len(set(filter(None, targets)))
    assert unique_targets >= 2, "メイドさんは異なる敵を分散してターゲットすべき"
```

### シナリオ10: 武器の射程を考慮したターゲット選択

```python
def test_scenario10_weapon_range_consideration():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "bow", Position(1, 1))      # 弓装備
    maid_b = test.add_maid("MaidB", "sword", Position(-1, 1))   # 剣装備
    skeleton = test.add_enemy("Skeleton", "skeleton", Position(5, 0))  # 遠い
    zombie = test.add_enemy("Zombie", "zombie", Position(1, 0))        # 近い
    
    # 検証: 弓メイドさんは遠い敵、剣メイドさんは近い敵を優先
    test.assert_target_priority(maid_a, skeleton, "弓は遠距離敵を優先")
    test.assert_target_priority(maid_b, zombie, "剣は近距離敵を優先")
```

### シナリオ11: エンダーマンへの対応

```python
def test_scenario11_enderman_response():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "bow", Position(1, 1))      # 弓装備
    maid_b = test.add_maid("MaidB", "sword", Position(-1, 1))   # 剣装備
    enderman = test.add_enemy("Enderman", "enderman", Position(2, 0))
    zombie = test.add_enemy("Zombie", "zombie", Position(3, 0))
    
    # 検証: 弓メイドさんはエンダーマンを避けてゾンビをターゲット
    # 剣メイドさんはエンダーマンをターゲット
    test.assert_target_priority(maid_a, zombie, "弓はエンダーマンを回避")
    test.assert_target_priority(maid_b, enderman, "剣はエンダーマンを対応")
```

### シナリオ14: ウィザー出現時の非攻撃的対応

```python
def test_scenario14_wither_non_aggressive():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "bow", Position(1, 1))
    maid_b = test.add_maid("MaidB", "sword", Position(-1, 1))
    wither = test.add_enemy("Wither", "wither", Position(5, 0), is_dangerous=True)
    
    # ウィザーはまだ何も攻撃していない状態
    
    # 検証: 両メイドさんともウィザーをターゲットしない
    priorities_a = test.calculate_priorities(maid_a)
    priorities_b = test.calculate_priorities(maid_b)
    
    assert wither not in priorities_a or priorities_a[wither] == 0, "弓メイドさんはウィザーを先制攻撃しない"
    assert wither not in priorities_b or priorities_b[wither] == 0, "剣メイドさんはウィザーを先制攻撃しない"
```

### シナリオ15: ウォーデンにターゲットされた時の避難

```python
def test_scenario15_warden_evacuation():
    test = TargetingTestCase()
    
    # 状況設定
    master = test.set_master(Position(0, 0))
    maid_a = test.add_maid("MaidA", "sword", Position(1, 1))
    maid_b = test.add_maid("MaidB", "bow", Position(-1, 1))
    maid_c = test.add_maid("MaidC", "sword", Position(0, 2))
    warden = test.add_enemy("Warden", "warden", Position(2, 0), is_dangerous=True)
    
    # ウォーデンがメイドさんAをターゲット
    test.set_attack_action(warden, maid_a)
    
    # 検証: メイドさんAは避難モード（優先度0またはマイナス）
    priorities_a = test.calculate_priorities(maid_a)
    
    # ターゲットされたメイドさんは戦闘ではなく避難を優先
    assert warden not in priorities_a or priorities_a[warden] <= 0, "ターゲットされたメイドさんは避難優先"
```

---

## Phase2: 優先度算出アルゴリズム設計

### アルゴリズムの基本構造

```python
def calculate_enemy_priorities(maid, enemies, master, other_maids):
    """
    指定されたメイドさんから見た各敵の優先度を算出
    
    Args:
        maid: 判断するメイドさん
        enemies: 周囲の敵リスト
        master: ご主人の情報
        other_maids: 他のメイドさんのリスト
    
    Returns:
        dict: {mob: priority_score} の辞書
    """
    priorities = {}
    
    for mob in enemies:
        score = calculate_base_priority(mob, maid, master, other_maids)
        score += calculate_distance_modifier(mob, maid, master)
        score += calculate_weapon_compatibility(mob, maid)
        score += calculate_danger_modifier(mob, maid, master)
        score += calculate_distribution_modifier(mob, other_maids)
        
        priorities[mob] = max(0, score)  # 負の値は0にクランプ
    
    return priorities
```

### 基本優先度の算出

```python
def calculate_base_priority(mob, maid, master, other_maids):
    """基本優先度を階層化に基づいて算出"""
    
    # 特別優先度: 危険敵からの避難
    if mob.is_dangerous and mob.is_targeting(maid):
        return -1000  # 避難優先（負の値で戦闘回避）
    
    # 優先度1: 攻撃してきた相手
    if mob.is_targeting(maid):
        return 1000
    
    # 優先度2: ご主人を攻撃した相手
    if mob.is_targeting(master):
        return 900
    
    # 優先度3: 他メイドさんを攻撃した相手
    for other_maid in other_maids:
        if mob.is_targeting(other_maid):
            return 800
    
    # 優先度4: ご主人が攻撃した相手
    if master.is_targeting(mob):
        return 700
    
    # 優先度5: 負傷メイドさんの支援・カバー
    for other_maid in other_maids:
        if other_maid.health < 10 and other_maid.current_target == mob:
            return 600
    
    # 優先度6: 周囲のEnemy（危険敵は先制攻撃しない）
    if mob.is_dangerous:
        return 0  # 先制攻撃しない
    else:
        return 500
```

### 距離による修正値

```python
def calculate_distance_modifier(mob, maid, master):
    """距離に基づく優先度修正"""
    
    distance_to_maid = mob.position.distance_to(maid.position)
    distance_to_master = mob.position.distance_to(master.position)
    
    # 最大ターゲット距離チェック
    if distance_to_maid > MAX_TARGET_DISTANCE:
        return -500  # 距離が遠すぎる場合は大幅減点
    
    # 近距離ボーナス（メイドさんに近いほど高優先度）
    distance_bonus = max(0, 50 - distance_to_maid * 5)
    
    # ご主人との距離考慮（ご主人から離れすぎた敵は減点）
    master_distance_penalty = max(0, (distance_to_master - 10) * 2)
    
    return distance_bonus - master_distance_penalty
```

### 武器相性による修正値

```python
def calculate_weapon_compatibility(mob, maid):
    """武器とターゲットの相性による修正"""
    
    distance_to_enemy = mob.position.distance_to(maid.position)
    
    if maid.weapon_type == "bow":
        # 弓の場合
        if mob.type == "enderman":
            return -1000  # エンダーマンは完全回避
        elif distance_to_enemy > 3:
            return 30  # 遠距離ボーナス
        else:
            return -10  # 近距離ペナルティ
    
    elif maid.weapon_type == "sword":
        # 剣の場合
        if distance_to_enemy <= 3:
            return 20  # 近距離ボーナス
        else:
            return -5  # 遠距離ペナルティ
    
    return 0
```

### 危険敵による修正値

```python
def calculate_danger_modifier(mob, maid, master):
    """危険な敵に対する特殊修正"""
    
    if not mob.is_dangerous:
        return 0
    
    distance_to_enemy = mob.position.distance_to(maid.position)
    
    # 危険敵への基本ペナルティ
    danger_penalty = -100
    
    # 近距離武器での危険敵は更にペナルティ
    if maid.weapon_type == "sword" and distance_to_enemy < 5:
        danger_penalty -= 200
    
    # 攻撃されている場合は例外的に対応
    if mob.is_targeting(master) or mob.is_targeting(maid):
        danger_penalty += 300  # ペナルティ軽減
    
    return danger_penalty
```

### 分散攻撃による修正値

```python
def calculate_distribution_modifier(mob, other_maids):
    """他メイドさんとの分散攻撃を考慮した修正"""
    
    # 同じ敵をターゲットしている他メイドさんの数
    targeting_count = sum(1 for maid in other_maids if maid.current_target == mob)
    
    # 集中攻撃の回避
    distribution_penalty = targeting_count * 50
    
    return -distribution_penalty
```

### 定数定義

```python
# アルゴリズムで使用する定数
MAX_TARGET_DISTANCE = 20  # 最大ターゲット距離
DANGEROUS_ENEMIES = ["wither", "warden", "ravager", "ender_dragon"]  # 危険な敵
RANGED_IMMUNE_ENEMIES = ["enderman"]  # 遠距離攻撃無効な敵
```

---

## Phase3: 追加設計作業

### 3.1 動的再評価システム

```python
class TargetingManager:
    def __init__(self):
        self.last_evaluation_time = 0
        self.evaluation_interval = 60  # 3秒（20tick/秒）
    
    def should_reevaluate(self, current_time):
        return current_time - self.last_evaluation_time >= self.evaluation_interval
    
    def force_reevaluation(self):
        """緊急事態での強制再評価"""
        self.last_evaluation_time = 0
```

### 3.2 状態管理システム

```python
class MaidCombatState:
    def __init__(self):
        self.current_target = None
        self.last_target_change = 0
        self.combat_mode = "normal"  # normal, supporting, evacuating
        self.weapon_range = self.calculate_weapon_range()
    
    def can_change_target(self, current_time):
        # 頻繁なターゲット変更を防ぐ
        return current_time - self.last_target_change > 20  # 1秒
```

### 3.3 パフォーマンス最適化

- **キャッシュシステム**: 距離計算結果のキャッシュ
- **段階的評価**: 基本優先度で足切りしてから詳細計算
- **範囲限定**: 一定距離外の敵は評価対象外

### 3.4 設定可能パラメータ

```python
class TargetingConfig:
    MAX_TARGET_DISTANCE = 20
    EVALUATION_INTERVAL = 60
    DANGER_DETECTION_RANGE = 10
    WEAPON_RANGE_BONUS = 30
    DISTRIBUTION_PENALTY = 50
```

### 3.5 デバッグ・テスト支援

- **ログ出力**: 優先度計算の詳細ログ
- **可視化**: ターゲット優先度のデバッグ表示
- **統計収集**: 戦闘効率の測定指標

### 3.6 既存システムとの統合

- **Goal優先度**: 既存のGoalシステムとの優先度調整
- **AI互換性**: 既存のAIゴールとの競合回避
- **イベント連携**: 攻撃・被攻撃イベントとの連携

### 3.7 拡張性の確保

- **敵タイプ定義**: 新しい敵タイプの容易な追加
- **戦術パターン**: 新しい戦術パターンの追加インターフェース
- **武器特性**: 新しい武器タイプへの対応

---

## 実装時の注意点

1. **パフォーマンス**: 大量のメイドさんでも動作する軽量設計
2. **安定性**: エッジケースでのクラッシュ回避
3. **調整性**: ゲームバランス調整のための設定外部化
4. **互換性**: 既存のメイドさんAIとの共存

---

## TDD実装完了サマリー

### 実装結果 ✅
- **全17シナリオ対応完了**
- **25テストケース実装**
- **バグ修正**: 負傷メイドさん支援の分散ペナルティ除外
- **テスト成功率**: 25/25 (100%)

### 新規追加シナリオ（TDD Phase1で実装）
1. **シナリオ4**: 複数メイドさんの分散攻撃
2. **シナリオ4-a**: 負傷メイドさんの支援・カバー
3. **シナリオ5**: 距離による自動ターゲット解除
4. **シナリオ6**: 優先度の動的変更
5. **シナリオ7**: ご主人が攻撃した敵への対応
6. **シナリオ8**: 集中攻撃の回避
7. **シナリオ9**: 他メイドさんへの攻撃対応
8. **シナリオ12**: 遠距離武器のポジショニング
9. **シナリオ13**: 遠距離無効敵の緊急対応
10. **シナリオ16**: ラヴェジャーとの距離戦
11. **シナリオ17**: エンダードラゴンへの協調遠距離攻撃

### アルゴリズム改善（Green Phase）
- **負傷メイドさん支援時の分散ペナルティ除外**
- **優先度計算の精度向上**
- **エッジケースのバグ修正**

### 実装環境
- **Python環境**: `/mnt/v/Develop/Minecraft/LMRB/scripts/targeting_system/`
- **テストフレームワーク**: pytest
- **実装手法**: t-wadaのTDD推奨（Red → Green → Refactor）

### 検証済み機能
- ✅ 基本戦闘システム（シナリオ1-3）
- ✅ 武器特性活用（シナリオ10-13）
- ✅ 危険敵への生存戦術（シナリオ14-17）
- ✅ チーム連携システム（シナリオ4-9）
- ✅ 距離・優先度管理（シナリオ5-6）

## 次のステップ

1. **Java移植**: LittleMaidEntityへの統合準備完了
2. **パラメータ調整**: ゲームバランス調整のための値調整
3. **パフォーマンス最適化**: 大規模戦闘での効率化
4. **実地テスト**: 実際のゲームプレイでの検証