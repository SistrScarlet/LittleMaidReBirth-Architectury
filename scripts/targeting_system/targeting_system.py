"""
メイドさん統合ターゲティングシステム v3.0

TDD完全実装: ご主人指示機能付き統合ターゲティングシステム

v3.0新機能:
- ご主人スタンス制御（護衛/援護モード）
- 敵1体への対応人数制御
- 援護モードでのご主人戦闘回避システム
- 後方互換性完全維持
"""

import math
from typing import List, Dict, Optional
from enum import Enum


# 定数定義 (t-wada メソッドに基づく改善)
MAX_PREEMPTIVE_DISTANCE = 16  # 最大先制ターゲット距離
MAX_TARGET_DISTANCE = 24      # 最大ターゲット可能距離
RANGE_THRESHOLD = 8           # 近距離/遠距離のしきい値

# 優先度定数
PRIORITY_EVACUATION = -1000      # 避難優先
PRIORITY_SELF_ATTACKER = 1000    # 自分を攻撃した相手
PRIORITY_MASTER_ATTACKER = 900   # ご主人を攻撃した相手
PRIORITY_MAID_ATTACKER = 800     # 他メイドさんを攻撃した相手
PRIORITY_MASTER_TARGET = 700     # ご主人が攻撃した相手
PRIORITY_INJURED_SUPPORT = 600   # 負傷メイドさんの支援
PRIORITY_NORMAL_ENEMY = 500      # 周囲の通常敵

# 武器特性定数
BOW_LONG_RANGE_THRESHOLD = 6     # 弓の遠距離ボーナス閾値
BOW_MID_RANGE_THRESHOLD = 4      # 弓の中間距離閾値
SWORD_CLOSE_RANGE_THRESHOLD = 4  # 剣の近距離ボーナス閾値


class MasterStance(Enum):
    """ご主人のスタンス設定"""
    GUARD = "guard"      # 護衛：ご主人保護優先（現状維持）
    SUPPORT = "support"  # 援護：ご主人の戦闘を邪魔せず周囲処理


class CombatSettings:
    """戦闘設定クラス"""
    
    def __init__(self, 
                 master_stance: MasterStance = MasterStance.GUARD,
                 max_maids_per_enemy: int = 2,
                 combat_zone_radius: float = 5.0):
        self.master_stance = master_stance
        self.max_maids_per_enemy = max_maids_per_enemy
        self.combat_zone_radius = combat_zone_radius


class Position:
    """3D位置を表すクラス"""
    
    def __init__(self, x: float, y: float, z: float = 0):
        self.x = x
        self.y = y
        self.z = z
    
    def distance_to(self, other: 'Position') -> float:
        """他の位置との距離を計算"""
        return math.sqrt(
            (self.x - other.x) ** 2 + 
            (self.y - other.y) ** 2 + 
            (self.z - other.z) ** 2
        )
    
    def __eq__(self, other):
        if not isinstance(other, Position):
            return False
        return (self.x == other.x and 
                self.y == other.y and 
                self.z == other.z)
    
    def __hash__(self):
        return hash((self.x, self.y, self.z))


class Entity:
    """基本エンティティクラス"""
    
    def __init__(self, name: str, position: Position):
        self.name = name
        self.position = position
        self.current_target: Optional['Entity'] = None
    
    def set_target(self, target: 'Entity'):
        """ターゲットを設定"""
        self.current_target = target
    
    def is_targeting(self, target: 'Entity') -> bool:
        """指定されたエンティティをターゲットしているかチェック"""
        return self.current_target == target


class Maid(Entity):
    """メイドさんクラス"""
    
    def __init__(self, name: str, weapon_type: str, position: Position, health: int = 20):
        super().__init__(name, position)
        self.weapon_type = weapon_type  # "sword", "bow", etc.
        self.health = health


class Enemy(Entity):
    """敵エンティティクラス"""
    
    def __init__(self, name: str, enemy_type: str, position: Position, is_dangerous: bool = False):
        super().__init__(name, position)
        self.type = enemy_type
        self.is_dangerous = is_dangerous


class Master(Entity):
    """ご主人クラス"""
    
    def __init__(self, position: Position):
        super().__init__("Master", position)


def calculate_enemy_priorities(maid: Maid, enemies: List[Enemy], master: Master, other_maids: List[Maid], settings: Optional[CombatSettings] = None) -> Dict[Enemy, float]:
    """
    指定されたメイドさんから見た各敵の優先度を算出
    
    Args:
        maid: 判断するメイドさん
        enemies: 周囲の敵リスト
        master: ご主人の情報
        other_maids: 他のメイドさんのリスト
        settings: 戦闘設定（オプション、デフォルトは護衛モード）
    
    Returns:
        dict: {enemy: priority_score} の辞書
    """
    if settings is None:
        settings = CombatSettings()  # デフォルトは護衛モード
    priorities = {}
    
    for enemy in enemies:
        score = calculate_base_priority(enemy, maid, master, other_maids)
        score += calculate_distance_modifier(enemy, maid, master)
        score += calculate_weapon_compatibility(enemy, maid)
        score += calculate_danger_modifier(enemy, maid, master)
        score += calculate_distribution_modifier(enemy, other_maids)
        score += calculate_assignment_modifier(enemy, other_maids, settings)
        score += calculate_master_stance_modifier(enemy, master, settings)
        
        # 避難優先度が基本にある場合は負の値を保持
        base_priority = calculate_base_priority(enemy, maid, master, other_maids)
        if base_priority == PRIORITY_EVACUATION:
            priorities[enemy] = score  # 避難優先度は負の値を保持
        else:
            priorities[enemy] = max(0, score)  # 通常は0以上にクランプ
    
    return priorities


def calculate_base_priority(enemy: Enemy, maid: Maid, master: Master, other_maids: List[Maid]) -> float:
    """基本優先度を階層化に基づいて算出"""
    
    distance_to_maid = enemy.position.distance_to(maid.position)
    
    # 特別優先度: 危険敵からの避難
    if enemy.is_dangerous and enemy.is_targeting(maid):
        return PRIORITY_EVACUATION  # 避難優先（負の値で戦闘回避）
    
    # 優先度1: 攻撃してきた相手
    if enemy.is_targeting(maid):
        return PRIORITY_SELF_ATTACKER
    
    # 優先度2: ご主人を攻撃した相手
    if enemy.is_targeting(master):
        return PRIORITY_MASTER_ATTACKER
    
    # 優先度3: 他メイドさんを攻撃した相手
    for other_maid in other_maids:
        if enemy.is_targeting(other_maid):
            return PRIORITY_MAID_ATTACKER
    
    # 優先度4: ご主人が攻撃した相手
    if master.is_targeting(enemy):
        return PRIORITY_MASTER_TARGET
    
    # 優先度5: 負傷メイドさんの支援・カバー
    for other_maid in other_maids:
        if other_maid.health < 10 and other_maid.current_target == enemy:
            return PRIORITY_INJURED_SUPPORT
    
    # 優先度6: 周囲のEnemy（危険敵は先制攻撃しない）
    if enemy.is_dangerous:
        return 0  # 先制攻撃しない
    else:
        # 先制攻撃距離制限
        if distance_to_maid > MAX_PREEMPTIVE_DISTANCE:
            return 0  # 先制攻撃範囲外
        return PRIORITY_NORMAL_ENEMY


def calculate_distance_modifier(enemy: Enemy, maid: Maid, master: Master) -> float:
    """距離に基づく優先度修正"""
    
    distance_to_maid = enemy.position.distance_to(maid.position)
    distance_to_master = enemy.position.distance_to(master.position)
    
    # 最大ターゲット距離チェック
    if distance_to_maid > MAX_TARGET_DISTANCE:
        return -500  # 距離が遠すぎる場合は大幅減点
    
    # 近距離ボーナス（メイドさんに近いほど高優先度）
    distance_bonus = max(0, (10 - distance_to_maid) * 5)
    
    # ご主人との距離考慮（ご主人から離れすぎた敵は減点）
    master_distance_penalty = max(0, (distance_to_master - 10) * 2)
    
    return distance_bonus - master_distance_penalty


def calculate_weapon_compatibility(enemy: Enemy, maid: Maid) -> float:
    """武器とターゲットの相性による修正"""
    
    distance_to_enemy = enemy.position.distance_to(maid.position)
    
    if maid.weapon_type == "bow":
        # 弓の場合
        if enemy.type == "enderman":
            return -1000  # エンダーマンは完全回避
        elif distance_to_enemy >= BOW_LONG_RANGE_THRESHOLD:  # 6ブロック以上で遠距離ボーナス
            bonus = 50  # 30 -> 50に強化
            # 危険敵への遠距離アドバンテージ
            if enemy.is_dangerous:
                bonus += 20
            return bonus
        elif distance_to_enemy >= BOW_MID_RANGE_THRESHOLD:  # 4-5ブロックは中間距離
            return 10
        else:
            return -10  # 3ブロック以下は近距離ペナルティ
    
    elif maid.weapon_type == "sword":
        # 剣の場合
        if distance_to_enemy <= SWORD_CLOSE_RANGE_THRESHOLD:  # 4ブロック以下で近距離ボーナス
            return 20
        elif distance_to_enemy <= RANGE_THRESHOLD:
            return 5  # 中間距離は小さなボーナス
        else:
            penalty = -5  # 遠距離ペナルティ
            # 危険敵への近接リスク
            if enemy.is_dangerous and distance_to_enemy < 6:
                penalty -= 30  # 危険敵に近づくリスクペナルティ
            return penalty
    
    return 0


def calculate_danger_modifier(enemy: Enemy, maid: Maid, master: Master) -> float:
    """危険な敵に対する特殊修正"""
    
    if not enemy.is_dangerous:
        return 0
    
    distance_to_enemy = enemy.position.distance_to(maid.position)
    
    # 危険敵への基本ペナルティ
    danger_penalty = -100
    
    # 近距離武器での危険敵は更にペナルティ
    if maid.weapon_type == "sword" and distance_to_enemy < 5:
        danger_penalty -= 200
    
    # 攻撃されている場合は例外的に対応
    if enemy.is_targeting(master) or enemy.is_targeting(maid):
        danger_penalty += 300  # ペナルティ軽減
    
    return danger_penalty


def calculate_distribution_modifier(enemy: Enemy, other_maids: List[Maid]) -> float:
    """他メイドさんとの分散攻撃を考慮した修正"""
    
    # 同じ敵をターゲットしている他メイドさんの数
    targeting_count = sum(1 for maid in other_maids if maid.current_target == enemy)
    
    # 負傷メイドさん支援の詳細チェック
    injured_maids_targeting = [maid for maid in other_maids if maid.health < 10 and maid.current_target == enemy]
    healthy_maids_targeting = [maid for maid in other_maids if maid.health >= 10 and maid.current_target == enemy]
    
    if injured_maids_targeting:
        # 負傷メイドさんがいる場合：健康なメイドさん1体のみ支援を許可
        if len(healthy_maids_targeting) == 0:
            return 0  # 支援なし：ペナルティなし
        elif len(healthy_maids_targeting) == 1:
            return -25  # 支援1体：軽いペナルティ
        else:
            return -120  # 過剰支援：重いペナルティ（強化）
    
    # 通常の集中攻撃回避（強化）
    distribution_penalty = targeting_count * 80  # 50 -> 80に強化
    
    return -distribution_penalty


def calculate_assignment_modifier(enemy: Enemy, other_maids: List[Maid], settings: CombatSettings) -> float:
    """敵1体への対応人数制御による修正"""
    
    # 同じ敵をターゲットしている他メイドさんの数
    targeting_count = sum(1 for maid in other_maids if maid.current_target == enemy)
    
    # 対応人数上限チェック
    if targeting_count >= settings.max_maids_per_enemy:
        # 上限到達時は大幅ペナルティ
        excess_count = targeting_count - settings.max_maids_per_enemy + 1
        return -excess_count * 100
    
    return 0  # 上限内は修正なし


def calculate_master_stance_modifier(enemy: Enemy, master: Master, settings: CombatSettings) -> float:
    """ご主人のスタンスに基づく優先度修正"""
    
    if settings.master_stance == MasterStance.GUARD:
        # 護衛モード：現状維持（修正なし）
        return 0
    
    elif settings.master_stance == MasterStance.SUPPORT:
        # 援護モード：ご主人の戦闘を邪魔しない
        if master.is_targeting(enemy):
            # ご主人がターゲットしている敵は低優先度に
            return -400  # 700→300に減少させる
        else:
            # ご主人以外の敵は優先度アップ
            return 100   # 500→600に増強
    
    return 0