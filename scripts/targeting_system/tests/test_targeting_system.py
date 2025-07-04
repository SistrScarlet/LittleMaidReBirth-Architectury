"""
メイドさん統合ターゲティングシステムのテスト

TDD手法に従って実装:
1. Red: 失敗するテストを書く
2. Green: テストを通す最小限のコードを書く 
3. Refactor: コードを改善する
"""

import pytest
from targeting_system import (
    Position, Maid, Enemy, Master, calculate_enemy_priorities,
    MasterStance, CombatSettings,
    # 定数のインポート (t-wada メソッドに基づく改善)
    MAX_PREEMPTIVE_DISTANCE, MAX_TARGET_DISTANCE, RANGE_THRESHOLD,
    PRIORITY_EVACUATION, PRIORITY_SELF_ATTACKER, PRIORITY_MASTER_ATTACKER,
    PRIORITY_MAID_ATTACKER, PRIORITY_MASTER_TARGET, PRIORITY_INJURED_SUPPORT,
    PRIORITY_NORMAL_ENEMY, BOW_LONG_RANGE_THRESHOLD, BOW_MID_RANGE_THRESHOLD,
    SWORD_CLOSE_RANGE_THRESHOLD
)


# t-wada メソッドに基づく改善: pytest fixture
@pytest.fixture
def basic_positions():
    """基本的な位置情報を提供するfixture"""
    return {
        'center': Position(0, 0),
        'north': Position(0, 1),
        'south': Position(0, -1),
        'east': Position(1, 0),
        'west': Position(-1, 0),
        'northeast': Position(1, 1),
        'northwest': Position(-1, 1),
        'far_north': Position(0, 10),
        'very_far_north': Position(0, 25)
    }


@pytest.fixture
def basic_entities(basic_positions):
    """基本的なエンティティを提供するfixture"""
    return {
        'master': Master(basic_positions['center']),
        'maid_sword': Maid("MaidSword", "sword", basic_positions['northeast']),
        'maid_bow': Maid("MaidBow", "bow", basic_positions['northwest']),
        'maid_injured': Maid("MaidInjured", "sword", basic_positions['north'], health=5),
        'zombie_close': Enemy("ZombieClose", "zombie", basic_positions['east']),
        'zombie_far': Enemy("ZombieFar", "zombie", basic_positions['far_north']),
        'skeleton_close': Enemy("SkeletonClose", "skeleton", basic_positions['west']),
        'skeleton_far': Enemy("SkeletonFar", "skeleton", basic_positions['very_far_north']),
        'enderman': Enemy("Enderman", "enderman", basic_positions['south']),
        'wither': Enemy("Wither", "wither", basic_positions['far_north'], is_dangerous=True),
        'warden': Enemy("Warden", "warden", basic_positions['east'], is_dangerous=True)
    }


@pytest.fixture
def combat_settings_guard():
    """護衛モードの戦闘設定を提供するfixture"""
    return CombatSettings(master_stance=MasterStance.GUARD, max_maids_per_enemy=2)


@pytest.fixture
def combat_settings_support():
    """援護モードの戦闘設定を提供するfixture"""
    return CombatSettings(master_stance=MasterStance.SUPPORT, max_maids_per_enemy=2)


class TestTargetingSystem:
    """ターゲティングシステムのテストクラス"""
    
    # t-wada メソッドに基づく改善: helper methods
    def _get_highest_priority_enemy(self, priorities: dict) -> Enemy:
        """最高優先度の敵を取得するhelper method"""
        return max(priorities, key=priorities.get)
    
    def _calculate_priorities(self, maid: Maid, enemies: list, master: Master, 
                            other_maids: list = None, settings: CombatSettings = None) -> dict:
        """優先度計算のhelper method"""
        if other_maids is None:
            other_maids = []
        return calculate_enemy_priorities(maid, enemies, master, other_maids, settings)
    
    def _assert_priority_order(self, priorities: dict, higher_enemy: Enemy, lower_enemy: Enemy, message: str):
        """優先度の順序をアサートするhelper method"""
        assert priorities[higher_enemy] > priorities[lower_enemy], \
            f"{message}: {higher_enemy.name}({priorities[higher_enemy]:.1f}) > {lower_enemy.name}({priorities[lower_enemy]:.1f})"
    
    def _create_combat_scenario(self, master_pos=(0, 0), maid_pos=(1, 1), weapon_type="sword"):
        """標準的な戦闘シナリオを作成するhelper method"""
        return {
            'master': Master(Position(*master_pos)),
            'maid': Maid("TestMaid", weapon_type, Position(*maid_pos)),
            'enemies': [],
            'other_maids': []
        }
    
    def test_scenario1_basic_attack_response(self):
        """シナリオ1: 基本的な攻撃反応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        zombie = Enemy("Zombie", "zombie", Position(0, 1))
        
        # ゾンビがご主人を攻撃
        zombie.set_target(master)
        
        # 検証: メイドさんは即座にゾンビをターゲット
        enemies = [zombie]
        priorities = self._calculate_priorities(maid_a, enemies, master)
        
        # ゾンビが最高優先度であることを確認
        assert zombie in priorities
        assert priorities[zombie] >= PRIORITY_MASTER_ATTACKER, \
            f"ご主人攻撃者は{PRIORITY_MASTER_ATTACKER}以上の優先度を持つべき: 実際={priorities[zombie]}"
        
        # 最も優先度の高い敵がゾンビであることを確認
        highest_priority_enemy = self._get_highest_priority_enemy(priorities)
        assert highest_priority_enemy == zombie, \
            f"ご主人を攻撃したゾンビが最優先ターゲットであるべき: 実際={highest_priority_enemy.name}"
    
    def test_scenario2_multiple_enemy_priority(self):
        """シナリオ2: 複数敵の優先度判定 - 距離による優先度"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        skeleton = Enemy("Skeleton", "skeleton", Position(12, 0))  # 12ブロック先
        zombie = Enemy("Zombie", "zombie", Position(5, 0))        # 5ブロック先
        
        # 検証: より近いゾンビを優先
        enemies = [skeleton, zombie]
        priorities = self._calculate_priorities(maid_a, enemies, master)
        
        assert zombie in priorities
        assert skeleton in priorities
        self._assert_priority_order(priorities, zombie, skeleton, "距離による優先度")
        
        # 具体的な距離情報も確認
        zombie_distance = zombie.position.distance_to(maid_a.position)
        skeleton_distance = skeleton.position.distance_to(maid_a.position)
        assert zombie_distance < skeleton_distance, \
            f"距離確認: ゾンビ({zombie_distance:.1f}) < スケルトン({skeleton_distance:.1f})"
    
    def test_scenario3_attacker_priority(self):
        """シナリオ3: 攻撃者への優先対応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        skeleton = Enemy("Skeleton", "skeleton", Position(12, 0))  # 遠い
        zombie = Enemy("Zombie", "zombie", Position(5, 0))        # 近い
        
        # スケルトンがご主人を攻撃（距離は遠いが攻撃者）
        skeleton.set_target(master)
        
        # 検証: 距離は遠いが攻撃者のスケルトンを優先
        enemies = [skeleton, zombie]
        other_maids = []
        priorities = calculate_enemy_priorities(maid_a, enemies, master, other_maids)
        
        assert priorities[skeleton] > priorities[zombie], "攻撃者は距離が遠くても優先されるべき"
    
    def test_scenario10_weapon_range_consideration(self):
        """シナリオ10: 武器の射程を考慮したターゲット選択"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow = Maid("MaidBow", "bow", Position(1, 1))      # 弓装備
        maid_sword = Maid("MaidSword", "sword", Position(-1, 1))   # 剣装備
        skeleton = Enemy("Skeleton", "skeleton", Position(12, 0))  # 遠い
        zombie = Enemy("Zombie", "zombie", Position(5, 0))        # 近い
        
        enemies = [skeleton, zombie]
        other_maids = []
        
        # 弓メイドさんの優先度計算
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids)
        
        # 剣メイドさんの優先度計算  
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids)
        
        # 検証: 弓メイドさんは遠い敵により適性を持つ
        # （近距離ペナルティにより、弓は近い敵より遠い敵を比較的好む）
        bow_skeleton_priority = bow_priorities[skeleton]
        bow_zombie_priority = bow_priorities[zombie]
        
        # 剣メイドさんは近い敵により適性を持つ
        sword_skeleton_priority = sword_priorities[skeleton]
        sword_zombie_priority = sword_priorities[zombie]
        
        # 距離による基本補正があるので、武器特性の差を確認
        bow_weapon_bonus_skeleton = bow_skeleton_priority - sword_skeleton_priority  
        bow_weapon_bonus_zombie = bow_zombie_priority - sword_zombie_priority
        
        # 弓は遠距離でボーナス、近距離でペナルティがあるはず
        assert bow_weapon_bonus_skeleton > bow_weapon_bonus_zombie, "弓は遠距離敵により適している"
    
    def test_scenario11_enderman_response(self):
        """シナリオ11: エンダーマンへの対応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow = Maid("MaidBow", "bow", Position(1, 1))      # 弓装備
        maid_sword = Maid("MaidSword", "sword", Position(-1, 1))   # 剣装備
        enderman = Enemy("Enderman", "enderman", Position(5, 0))
        zombie = Enemy("Zombie", "zombie", Position(8, 0))
        
        enemies = [enderman, zombie]
        other_maids = []
        
        # 検証: 弓メイドさんはエンダーマンを避けてゾンビをターゲット
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids)
        bow_target = max(bow_priorities, key=bow_priorities.get)
        assert bow_target == zombie, "弓メイドさんはエンダーマンを回避してゾンビをターゲット"
        
        # 剣メイドさんはエンダーマンをターゲット可能
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids)
        # エンダーマンも有効なターゲットである（武器制約なし）
        assert enderman in sword_priorities
        assert sword_priorities[enderman] > 0, "剣メイドさんはエンダーマンをターゲット可能"
    
    def test_scenario14_dangerous_enemy_non_aggressive(self):
        """シナリオ14: 危険敵（ウィザー）への非攻撃的対応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow = Maid("MaidBow", "bow", Position(1, 1))
        maid_sword = Maid("MaidSword", "sword", Position(-1, 1))
        wither = Enemy("Wither", "wither", Position(5, 0), is_dangerous=True)
        
        # ウィザーはまだ何も攻撃していない状態
        enemies = [wither]
        other_maids = []
        
        # 検証: 両メイドさんともウィザーを先制攻撃しない
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids)
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids)
        
        # 危険敵は基本優先度0 + 危険修正 = 非常に低い優先度
        assert bow_priorities[wither] <= 50, "弓メイドさんはウィザーを先制攻撃しない"
        assert sword_priorities[wither] <= 50, "剣メイドさんはウィザーを先制攻撃しない"
    
    def test_scenario15_warden_evacuation(self):
        """シナリオ15: ウォーデンにターゲットされた時の避難"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_target = Maid("MaidTarget", "sword", Position(1, 1))
        maid_other = Maid("MaidOther", "bow", Position(-1, 1))
        warden = Enemy("Warden", "warden", Position(5, 0), is_dangerous=True)
        
        # ウォーデンがメイドさんをターゲット
        warden.set_target(maid_target)
        
        enemies = [warden]
        other_maids = [maid_other]
        
        # 検証: ターゲットされたメイドさんは避難モード（負の優先度）
        target_priorities = self._calculate_priorities(maid_target, enemies, master, other_maids)
        
        # 危険敵にターゲットされた場合は避難優先（負の値）
        assert target_priorities[warden] < 0, \
            f"危険敵にターゲットされた場合は避難優先（負の値）: 実際={target_priorities[warden]}"
        assert target_priorities[warden] < -500, \
            f"避難優先度は十分に低いべき（-500未満）: 実際={target_priorities[warden]}"
    
    def test_multiple_maids_distribution(self):
        """複数メイドさんの分散攻撃テスト"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        maid_b = Maid("MaidB", "sword", Position(-1, 1))  
        maid_c = Maid("MaidC", "sword", Position(0, 2))
        
        zombie1 = Enemy("Zombie1", "zombie", Position(5, 0))
        zombie2 = Enemy("Zombie2", "zombie", Position(8, 0))
        
        # メイドさんBとCが既にzombie1をターゲットしている状況
        maid_b.set_target(zombie1)
        maid_c.set_target(zombie1)
        
        enemies = [zombie1, zombie2]
        other_maids = [maid_b, maid_c]
        
        # メイドさんAの優先度計算
        priorities_a = calculate_enemy_priorities(maid_a, enemies, master, other_maids)
        
        # 検証: 集中攻撃を避けるため、zombie2の方が優先度が高くなるべき
        # zombie1は2体のメイドさんがターゲット中なので分散ペナルティ
        assert priorities_a[zombie2] > priorities_a[zombie1], "集中攻撃を避けて分散すべき"
    
    def test_scenario4_multiple_maids_distribution(self):
        """シナリオ4: 複数メイドさんの分散攻撃"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        maid_b = Maid("MaidB", "sword", Position(-1, 1))
        maid_c = Maid("MaidC", "sword", Position(0, 2))
        zombie1 = Enemy("Zombie1", "zombie", Position(5, 0))
        zombie2 = Enemy("Zombie2", "zombie", Position(8, 0))
        
        enemies = [zombie1, zombie2]
        other_maids_a = [maid_b, maid_c]
        other_maids_b = [maid_a, maid_c]
        other_maids_c = [maid_a, maid_b]
        
        # 各メイドさんの優先度計算・最優先ターゲット
        priorities_a = calculate_enemy_priorities(maid_a, enemies, master, other_maids_a)
        target_a = max(priorities_a, key=priorities_a.get)
        maid_a.set_target(target_a)

        priorities_b = calculate_enemy_priorities(maid_b, enemies, master, other_maids_b)
        target_b = max(priorities_b, key=priorities_b.get)
        maid_b.set_target(target_b)

        priorities_c = calculate_enemy_priorities(maid_c, enemies, master, other_maids_c)
        target_c = max(priorities_c, key=priorities_c.get)
        maid_c.set_target(target_c)
        
        # 検証: 3体が異なる敵を分散してターゲット、または1体は待機
        targets = [target_a, target_b, target_c]
        unique_targets = len(set(targets))
        assert unique_targets == len(enemies), "2体の敵がターゲットされるべき"
    
    def test_scenario4a_injured_maid_support(self):
        """シナリオ4-a: 負傷メイドさんの支援・カバー"""
        # 状況設定  
        master = Master(Position(0, 0))
        maid_injured = Maid("MaidInjured", "sword", Position(3, 0), health=5)  # 低体力
        maid_supporter = Maid("MaidSupporter", "sword", Position(-1, 1), health=20)
        zombie = Enemy("Zombie", "zombie", Position(5, 0))
        
        # 負傷メイドさんが既にゾンビをターゲット
        maid_injured.set_target(zombie)
        
        enemies = [zombie]
        other_maids = [maid_injured]
        
        # 支援メイドさんの優先度計算
        priorities = calculate_enemy_priorities(maid_supporter, enemies, master, other_maids)
        
        # 検証: 負傷メイドさんの敵を支援
        assert zombie in priorities
        assert priorities[zombie] > 500, "負傷メイドさんの敵を支援すべき"
    
    def test_scenario5_distance_based_target_release(self):
        """シナリオ5: 距離による自動ターゲット解除"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "sword", Position(1, 1))
        skeleton_far = Enemy("SkeletonFar", "skeleton", Position(25, 0))  # 25ブロック離れた
        zombie_close = Enemy("ZombieClose", "zombie", Position(5, 0))     # 近くの新しい敵
        
        enemies = [skeleton_far, zombie_close]
        other_maids = []
        
        # 検証: 遠すぎる敵は優先度が0またはマイナス
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids)
        
        assert priorities[skeleton_far] <= 0, "遠すぎる敵はターゲット解除されるべき"
        assert priorities[zombie_close] > priorities[skeleton_far], "近い敵を優先すべき"
    
    def test_scenario6_dynamic_priority_change(self):
        """シナリオ6: 優先度の動的変更"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "sword", Position(1, 1))
        skeleton_far = Enemy("SkeletonFar", "skeleton", Position(8, 0))
        creeper = Enemy("Creeper", "creeper", Position(3, 0))
        
        # 最初：スケルトンのみ存在
        enemies_initial = [skeleton_far]
        priorities_initial = calculate_enemy_priorities(maid, enemies_initial, master, [])
        
        # 突然クリーパーがご主人を攻撃
        creeper.set_target(master)
        enemies_updated = [skeleton_far, creeper]
        priorities_updated = calculate_enemy_priorities(maid, enemies_updated, master, [])
        
        # 検証: 攻撃者であるクリーパーが最優先に変更
        assert priorities_updated[creeper] > priorities_updated[skeleton_far], "攻撃者を即座に最優先すべき"
    
    def test_scenario7_master_target_support(self):
        """シナリオ7: ご主人が攻撃した敵への対応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "sword", Position(1, 1))
        enderman = Enemy("Enderman", "enderman", Position(3, 0))
        zombie = Enemy("Zombie", "zombie", Position(4, 0))
        
        # ご主人がエンダーマンを攻撃
        master.set_target(enderman)
        
        enemies = [enderman, zombie]
        other_maids = []
        
        # 検証: ご主人がターゲットした敵を支援
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids)
        
        assert priorities[enderman] > priorities[zombie], "ご主人がターゲットした敵を支援すべき"
    
    def test_scenario8_concentrate_attack_avoidance(self):
        """シナリオ8: 集中攻撃の回避（正式版）"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        maid_b = Maid("MaidB", "sword", Position(-1, 1))
        maid_c = Maid("MaidC", "sword", Position(0, 2))
        
        zombie1 = Enemy("Zombie1", "zombie", Position(3, 0))
        zombie2 = Enemy("Zombie2", "zombie", Position(5, 1))
        skeleton = Enemy("Skeleton", "skeleton", Position(5, -1))
        
        # 全メイドさんが同じゾンビをターゲットしている状況を模擬
        maid_b.set_target(zombie1)
        maid_c.set_target(zombie1)
        
        enemies = [zombie1, zombie2, skeleton]
        other_maids = [maid_b, maid_c]
        
        # メイドさんAの優先度計算
        priorities = calculate_enemy_priorities(maid_a, enemies, master, other_maids)
        
        # 検証: 集中攻撃を避けて分散
        target_a = max(priorities, key=priorities.get)
        assert target_a != zombie1, "集中攻撃されている敵は避けるべき"
    
    def test_scenario9_maid_attack_response(self):
        """シナリオ9: 他メイドさんへの攻撃対応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        maid_b = Maid("MaidB", "sword", Position(-1, 1))
        maid_c = Maid("MaidC", "sword", Position(0, 2))
        
        zombie = Enemy("Zombie", "zombie", Position(5, 0))
        skeleton = Enemy("Skeleton", "skeleton", Position(8, 0))
        
        # メイドさんAがゾンビと戦闘中、スケルトンがメイドさんAを攻撃
        maid_a.set_target(zombie)
        skeleton.set_target(maid_a)
        
        enemies = [zombie, skeleton]
        other_maids = [maid_a, maid_c]
        
        # メイドさんBの優先度計算
        priorities = calculate_enemy_priorities(maid_b, enemies, master, other_maids)
        
        # 検証: 他メイドさんを攻撃した敵を最優先
        assert priorities[skeleton] > priorities[zombie], "他メイドさんを攻撃した敵を最優先すべき"
    
    def test_scenario12_ranged_weapon_positioning(self):
        """シナリオ12: 遠距離武器のポジショニング"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow = Maid("MaidBow", "bow", Position(1, 1))
        maid_sword = Maid("MaidSword", "sword", Position(-1, 1))
        
        # 複数の敵が接近
        enemy1 = Enemy("Enemy1", "zombie", Position(2, 0))
        enemy2 = Enemy("Enemy2", "skeleton", Position(4, 0))
        enemy3 = Enemy("Enemy3", "creeper", Position(6, 0))
        
        enemies = [enemy1, enemy2, enemy3]
        other_maids_bow = [maid_sword]
        other_maids_sword = [maid_bow]
        
        # 優先度計算
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids_bow)
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids_sword)
        
        # 検証: 弓は遠距離敵を、剣は近距離敵を優先する傾向
        bow_target = max(bow_priorities, key=bow_priorities.get)
        sword_target = max(sword_priorities, key=sword_priorities.get)
        
        # 弓は比較的遠い敵を好む
        bow_distance = bow_target.position.distance_to(maid_bow.position)
        sword_distance = sword_target.position.distance_to(maid_sword.position)
        
        # 武器特性の違いを確認
        assert bow_distance >= sword_distance or bow_target != sword_target, "武器特性に応じた役割分担"
    
    def test_scenario13_ranged_immune_emergency(self):
        """シナリオ13: 遠距離無効敵の緊急対応"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow = Maid("MaidBow", "bow", Position(1, 1))
        maid_sword = Maid("MaidSword", "sword", Position(-1, 1))
        
        skeleton_far = Enemy("SkeletonFar", "skeleton", Position(8, 0))
        enderman = Enemy("Enderman", "enderman", Position(2, 0))
        
        # エンダーマンがご主人を攻撃（緊急事態）
        enderman.set_target(master)
        
        enemies = [skeleton_far, enderman]
        other_maids_bow = [maid_sword]
        other_maids_sword = [maid_bow]
        
        # 優先度計算
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids_bow)
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids_sword)
        
        # 検証: 弓はエンダーマンを回避、剣は緊急対応
        bow_target = max(bow_priorities, key=bow_priorities.get)
        sword_target = max(sword_priorities, key=sword_priorities.get)
        
        assert bow_target == skeleton_far, "弓はエンダーマンを回避してスケルトンを継続"
        assert sword_target == enderman, "剣は緊急事態のエンダーマンに対応"
    
    def test_scenario16_ravager_distance_combat(self):
        """シナリオ16: ラヴェジャーとの距離戦"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow1 = Maid("MaidBow1", "bow", Position(1, 1))
        maid_bow2 = Maid("MaidBow2", "bow", Position(-1, 1))
        maid_sword = Maid("MaidSword", "sword", Position(0, 2))
        
        ravager = Enemy("Ravager", "ravager", Position(5, 0), is_dangerous=True)
        
        # ラヴェジャーがご主人を攻撃
        ravager.set_target(master)
        
        enemies = [ravager]
        other_maids_bow1 = [maid_bow2, maid_sword]
        other_maids_sword = [maid_bow1, maid_bow2]
        
        # 優先度計算
        bow1_priorities = calculate_enemy_priorities(maid_bow1, enemies, master, other_maids_bow1)
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids_sword)
        
        # 検証: 弓は遠距離攻撃、剣は後方待機（低優先度）
        assert bow1_priorities[ravager] > 0, "弓は遠距離からラヴェジャーを攻撃"
        assert sword_priorities[ravager] < bow1_priorities[ravager], "剣は後方待機で低優先度"
    
    def test_scenario17_ender_dragon_coordinated_attack(self):
        """シナリオ17: エンダードラゴンへの協調遠距離攻撃"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow1 = Maid("MaidBow1", "bow", Position(1, 1))
        maid_bow2 = Maid("MaidBow2", "bow", Position(-1, 1))
        maid_sword = Maid("MaidSword", "sword", Position(0, 2))
        
        ender_dragon = Enemy("EnderDragon", "ender_dragon", Position(10, 10), is_dangerous=True)
        
        # ご主人が攻撃開始
        master.set_target(ender_dragon)
        
        enemies = [ender_dragon]
        other_maids_bow1 = [maid_bow2, maid_sword]
        other_maids_sword = [maid_bow1, maid_bow2]
        
        # 優先度計算
        bow1_priorities = calculate_enemy_priorities(maid_bow1, enemies, master, other_maids_bow1)
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids_sword)
        
        # 検証: 弓は遠距離支援、剣は防御待機
        assert bow1_priorities[ender_dragon] > 0, "弓は遠距離からエンダードラゴンを支援攻撃"
        assert sword_priorities[ender_dragon] >= 0, "剣は防御待機（ご主人のターゲットなので0以上）"
    
    def test_scenario18_preemptive_distance_boundary(self):
        """シナリオ18: 先制攻撃距離制限の境界テスト"""
        # 状況設定
        scenario = self._create_combat_scenario()
        
        # MAX_PREEMPTIVE_DISTANCE境界でのテスト
        zombie_inside = Enemy("ZombieInside", "zombie", Position(MAX_PREEMPTIVE_DISTANCE - 1, 0))  
        zombie_outside = Enemy("ZombieOutside", "zombie", Position(MAX_PREEMPTIVE_DISTANCE + 1, 0))
        
        enemies = [zombie_inside, zombie_outside]
        
        # 優先度計算
        priorities = self._calculate_priorities(scenario['maid'], enemies, scenario['master'])
        
        # 検証: 先制攻撃距離境界での制限
        distance_inside = zombie_inside.position.distance_to(scenario['maid'].position)
        distance_outside = zombie_outside.position.distance_to(scenario['maid'].position)
        
        print(f"境界内敵距離: {distance_inside:.1f}, 優先度: {priorities[zombie_inside]:.1f}")
        print(f"境界外敵距離: {distance_outside:.1f}, 優先度: {priorities[zombie_outside]:.1f}")
        print(f"MAX_PREEMPTIVE_DISTANCE: {MAX_PREEMPTIVE_DISTANCE}")
        
        # 先制攻撃距離制限の確認（距離による修正を考慮）
        assert priorities[zombie_inside] > 0, \
            f"範囲内の敵は先制攻撃可能であるべき: 距離{distance_inside:.1f}, 優先度{priorities[zombie_inside]}"
        assert priorities[zombie_outside] == 0, \
            f"範囲外の敵は先制攻撃不可であるべき: 距離{distance_outside:.1f}, 優先度{priorities[zombie_outside]}"
        assert priorities[zombie_inside] > priorities[zombie_outside], \
            f"境界値での明確な区別: 内側({priorities[zombie_inside]}) > 外側({priorities[zombie_outside]})"
    
    def test_scenario19_weapon_threshold_boundary(self):
        """シナリオ19: 武器特性閾値の境界テスト"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_bow = Maid("MaidBow", "bow", Position(1, 1))
        maid_sword = Maid("MaidSword", "sword", Position(-1, 1))
        
        # 6ブロック先（近距離側：弓にペナルティ、剣にボーナス）
        zombie1 = Enemy("Zombie1", "zombie", Position(6, 0))
        # 12ブロック先（遠距離側：弓にボーナス、剣にペナルティ）
        zombie2 = Enemy("Zombie2", "zombie", Position(12, 0))
        
        enemies = [zombie1, zombie2]
        other_maids_bow = [maid_sword]
        other_maids_sword = [maid_bow]
        
        # 優先度計算
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids_bow)
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids_sword)
        
        # 各メイドさんの最優先ターゲット
        bow_target = max(bow_priorities, key=bow_priorities.get)
        sword_target = max(sword_priorities, key=sword_priorities.get)
        
        # 検証: 武器特性による明確な分離
        bow_distance1 = zombie1.position.distance_to(maid_bow.position)
        bow_distance2 = zombie2.position.distance_to(maid_bow.position)
        sword_distance1 = zombie1.position.distance_to(maid_sword.position)
        sword_distance2 = zombie2.position.distance_to(maid_sword.position)
        
        print(f"弓: Zombie1({bow_distance1:.1f}ブロック)={bow_priorities[zombie1]:.1f}, Zombie2({bow_distance2:.1f}ブロック)={bow_priorities[zombie2]:.1f}")
        print(f"剣: Zombie1({sword_distance1:.1f}ブロック)={sword_priorities[zombie1]:.1f}, Zombie2({sword_distance2:.1f}ブロック)={sword_priorities[zombie2]:.1f}")
        print(f"弓の最優先: {bow_target.name}, 剣の最優先: {sword_target.name}")
        
        assert bow_target == zombie2, "弓は遠距離の敵を優先"
        assert sword_target == zombie1, "剣は近距離の敵を優先"
    
    def test_scenario20_max_distance_boundary(self):
        """シナリオ20: 最大ターゲット距離の境界テスト"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "bow", Position(1, 1))
        
        # 23ブロック先（最大距離内）
        skeleton_close = Enemy("SkeletonClose", "skeleton", Position(23, 0))
        # 25ブロック先（最大距離超過）
        skeleton_far = Enemy("SkeletonFar", "skeleton", Position(25, 0))
        # 近くの新しい敵
        zombie_new = Enemy("ZombieNew", "zombie", Position(5, 0))
        
        enemies = [skeleton_close, skeleton_far, zombie_new]
        other_maids = []
        
        # 優先度計算
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids)
        
        # 検証: 24ブロック境界での最大距離制限
        distance_close = skeleton_close.position.distance_to(maid.position)
        distance_far = skeleton_far.position.distance_to(maid.position)
        distance_new = zombie_new.position.distance_to(maid.position)
        
        print(f"スケルトン近: 距離{distance_close:.1f}, 優先度{priorities[skeleton_close]:.1f}")
        print(f"スケルトン遠: 距離{distance_far:.1f}, 優先度{priorities[skeleton_far]:.1f}")
        print(f"ゾンビ新: 距離{distance_new:.1f}, 優先度{priorities[zombie_new]:.1f}")
        
        # 最大距離制限の確認
        assert priorities[skeleton_close] > 0, "23ブロック先の敵はターゲット可能"
        assert priorities[skeleton_far] == 0, "25ブロック先の敵はターゲット不可"
        
        # 最優先ターゲットの確認
        highest_priority_enemy = max(priorities, key=priorities.get)
        assert highest_priority_enemy == zombie_new, "新しい近い敵が最優先"
    
    def test_scenario21_guard_mode_basic(self):
        """シナリオ21: 護衛モード基本動作（現状維持確認）"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "sword", Position(1, 1))
        zombie = Enemy("Zombie", "zombie", Position(5, 0))
        
        # ゾンビがご主人を攻撃
        zombie.set_target(master)
        
        enemies = [zombie]
        other_maids = []
        
        # 護衛モードでの優先度計算（新しい引数を追加）
        from targeting_system import MasterStance, CombatSettings
        settings = CombatSettings(master_stance=MasterStance.GUARD, max_maids_per_enemy=2)
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids, settings)
        
        # 検証: 護衛モードでは現状と同じ動作
        assert priorities[zombie] > 800, "護衛モードではご主人攻撃者を高優先度"
    
    def test_scenario22_guard_mode_priority(self):
        """シナリオ22: 護衛モード複数敵優先度"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "sword", Position(1, 1))
        zombie1 = Enemy("Zombie1", "zombie", Position(5, 0))
        zombie2 = Enemy("Zombie2", "zombie", Position(6, 0))
        
        # ゾンビ1がご主人を攻撃、ゾンビ2は通常敵
        zombie1.set_target(master)
        
        enemies = [zombie1, zombie2]
        other_maids = []
        
        # 護衛モードでの優先度計算
        from targeting_system import MasterStance, CombatSettings
        settings = CombatSettings(master_stance=MasterStance.GUARD, max_maids_per_enemy=2)
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids, settings)
        
        # 検証: ご主人攻撃者を優先
        assert priorities[zombie1] > priorities[zombie2], "護衛モードでもご主人攻撃者優先"
    
    def test_scenario23_guard_mode_assignment_limit(self):
        """シナリオ23: 護衛モード対応人数制御"""
        # 状況設定
        master = Master(Position(0, 0))
        maid_a = Maid("MaidA", "sword", Position(1, 1))
        maid_b = Maid("MaidB", "sword", Position(-1, 1))
        maid_c = Maid("MaidC", "sword", Position(0, 2))
        zombie = Enemy("Zombie", "zombie", Position(5, 0))
        
        # メイドB、Cが既にゾンビをターゲット（上限2体到達）
        maid_b.set_target(zombie)
        maid_c.set_target(zombie)
        
        enemies = [zombie]
        other_maids = [maid_b, maid_c]
        
        # 護衛モードで対応人数制御
        from targeting_system import MasterStance, CombatSettings
        settings = CombatSettings(master_stance=MasterStance.GUARD, max_maids_per_enemy=2)
        priorities = calculate_enemy_priorities(maid_a, enemies, master, other_maids, settings)
        
        # 検証: 上限到達時はペナルティ
        assert priorities[zombie] < 400, "対応人数上限到達でペナルティ"
    
    def test_scenario24_support_mode_avoid_master_target(self):
        """シナリオ24: 援護モードでご主人ターゲット回避"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "sword", Position(1, 1))
        zombie1 = Enemy("Zombie1", "zombie", Position(5, 0))
        zombie2 = Enemy("Zombie2", "zombie", Position(6, 0))
        
        # ご主人がゾンビ1をターゲット
        master.set_target(zombie1)
        
        enemies = [zombie1, zombie2]
        other_maids = []
        
        # 援護モードでの優先度計算
        from targeting_system import MasterStance, CombatSettings
        settings = CombatSettings(master_stance=MasterStance.SUPPORT, max_maids_per_enemy=2)
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids, settings)
        
        # 検証: ご主人のターゲットは低優先度、別の敵を優先
        assert priorities[zombie2] > priorities[zombie1], "援護モードではご主人ターゲット回避"
        assert priorities[zombie1] < 400, "ご主人ターゲット敵は低優先度"
    
    def test_scenario25_support_mode_periphery_priority(self):
        """シナリオ25: 援護モード周辺敵優先処理"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "bow", Position(1, 1))
        zombie_center = Enemy("ZombieCenter", "zombie", Position(1, 0))  # ご主人近く
        zombie_periphery = Enemy("ZombiePeriphery", "zombie", Position(8, 0))  # 周辺
        
        # ご主人が中央の敵をターゲット
        master.set_target(zombie_center)
        
        enemies = [zombie_center, zombie_periphery]
        other_maids = []
        
        # 援護モードでの優先度計算
        from targeting_system import MasterStance, CombatSettings
        settings = CombatSettings(master_stance=MasterStance.SUPPORT, max_maids_per_enemy=2)
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids, settings)
        
        # 検証: 周辺敵を優先
        assert priorities[zombie_periphery] > priorities[zombie_center], "援護モードでは周辺敵優先"
    
    def test_scenario26_support_mode_combat_zone_positioning(self):
        """シナリオ26: 援護モード戦闘エリア外ポジショニング"""
        # 状況設定
        master = Master(Position(0, 0))
        maid = Maid("Maid", "bow", Position(1, 1))
        zombie_close = Enemy("ZombieClose", "zombie", Position(4, 0))  # 戦闘エリア内
        zombie_far = Enemy("ZombieFar", "zombie", Position(10, 0))  # 戦闘エリア外
        
        # 両方とも通常敵（ご主人はターゲットしていない）
        enemies = [zombie_close, zombie_far]
        other_maids = []
        
        # 援護モードでの優先度計算
        from targeting_system import MasterStance, CombatSettings
        settings = CombatSettings(master_stance=MasterStance.SUPPORT, max_maids_per_enemy=2, combat_zone_radius=5.0)
        priorities = calculate_enemy_priorities(maid, enemies, master, other_maids, settings)
        
        # 検証: 戦闘エリア外の敵を優先
        assert priorities[zombie_far] > priorities[zombie_close], "援護モードでは戦闘エリア外優先"

    # t-wada メソッドに基づく改善: パラメータ化されたテスト
    @pytest.mark.parametrize("weapon_type,distance,expected_result", [
        ("bow", 4, "penalty"),    # 弓 + 近距離 = ペナルティ
        ("bow", 12, "bonus"),      # 弓 + 遠距離 = ボーナス
        ("bow", 8, "small_bonus"), # 弓 + 中距離 = 小ボーナス
        ("sword", 4, "bonus"),    # 剣 + 近距離 = ボーナス
        ("sword", 12, "penalty"), # 剣 + 遠距離 = ペナルティ
        ("sword", 8, "small_bonus"), # 剣 + 中距離 = 小ボーナス
    ])
    def test_weapon_distance_compatibility_parametrized(self, weapon_type, distance, expected_result):
        """武器と距離の相性テスト（パラメータ化）"""
        scenario = self._create_combat_scenario(weapon_type=weapon_type)
        enemy = Enemy("TestEnemy", "zombie", Position(distance, 0))
        
        priorities = self._calculate_priorities(
            scenario['maid'], [enemy], scenario['master'], scenario['other_maids']
        )
        
        # 相性なしの基準として、距離のみ考慮した値を取得
        neutral_maid = Maid("NeutralMaid", "unknown", Position(1, 1))
        neutral_priorities = self._calculate_priorities(
            neutral_maid, [enemy], scenario['master'], scenario['other_maids']
        )
        
        weapon_effect = priorities[enemy] - neutral_priorities[enemy]
        
        if expected_result == "bonus":
            assert weapon_effect > 0, f"{weapon_type}は距離{distance}でボーナス効果を持つべき"
        elif expected_result == "penalty":
            assert weapon_effect < 0, f"{weapon_type}は距離{distance}でペナルティ効果を持つべき"
        elif expected_result == "small_bonus":
            assert weapon_effect >= 0, f"{weapon_type}は距離{distance}で小ボーナス効果を持つべき"
    
    @pytest.mark.parametrize("enemy_type,weapon_type,expected_priority", [
        ("enderman", "bow", "avoid"),        # 弓 + エンダーマン = 回避
        ("enderman", "sword", "normal"),     # 剣 + エンダーマン = 通常
        ("zombie", "bow", "normal"),         # 弓 + ゾンビ = 通常
        ("zombie", "sword", "normal"),       # 剣 + ゾンビ = 通常
    ])
    def test_enemy_type_weapon_compatibility_parametrized(self, enemy_type, weapon_type, expected_priority):
        """敵種別と武器の相性テスト（パラメータ化）"""
        scenario = self._create_combat_scenario(weapon_type=weapon_type)
        enemy = Enemy("TestEnemy", enemy_type, Position(5, 0))
        
        priorities = self._calculate_priorities(
            scenario['maid'], [enemy], scenario['master'], scenario['other_maids']
        )
        
        if expected_priority == "avoid":
            assert priorities[enemy] <= 0, f"{weapon_type}は{enemy_type}を回避すべき"
        elif expected_priority == "normal":
            assert priorities[enemy] > 0, f"{weapon_type}は{enemy_type}を通常ターゲット可能"
    
    @pytest.mark.parametrize("distance,expected_targetable", [
        (MAX_PREEMPTIVE_DISTANCE - 3, True),   # 先制攻撃範囲内（距離修正考慮）
        (MAX_PREEMPTIVE_DISTANCE + 1, False),  # 先制攻撃範囲外
        (10, True),                            # 最大距離内で距離修正が正の値
        (MAX_TARGET_DISTANCE + 1, False),      # 最大距離外
    ])
    def test_distance_boundary_parametrized(self, distance, expected_targetable):
        """距離境界値テスト（パラメータ化）"""
        scenario = self._create_combat_scenario()
        enemy = Enemy("TestEnemy", "zombie", Position(distance, 0))
        
        priorities = self._calculate_priorities(
            scenario['maid'], [enemy], scenario['master'], scenario['other_maids']
        )
        
        if expected_targetable:
            assert priorities[enemy] > 0, f"距離{distance}の敵はターゲット可能であるべき"
        else:
            assert priorities[enemy] <= 0, f"距離{distance}の敵はターゲット不可であるべき"


if __name__ == "__main__":
    pytest.main([__file__])