"""
ターゲティングシステムの詳細分析テスト

実際の数値や動作を確認するためのテスト
"""

import pytest
from targeting_system import Position, Maid, Enemy, Master, calculate_enemy_priorities


class TestDetailedAnalysis:
    """詳細分析テストクラス"""
    
    def test_priority_score_analysis(self):
        """優先度スコアの詳細分析"""
        print("\n=== 優先度スコア詳細分析 ===")
        
        # 複雑なシナリオを設定
        master = Master(Position(0, 0))
        maid_bow = Maid("弓メイドさん", "bow", Position(2, 2))
        maid_sword = Maid("剣メイドさん", "sword", Position(-2, 2))
        
        # 様々な敵を配置
        zombie_close = Enemy("近いゾンビ", "zombie", Position(1, 1))
        skeleton_far = Enemy("遠いスケルトン", "skeleton", Position(8, 8))
        enderman = Enemy("エンダーマン", "enderman", Position(3, 3))
        wither = Enemy("ウィザー", "wither", Position(10, 10), is_dangerous=True)
        
        # スケルトンがご主人を攻撃
        skeleton_far.set_target(master)
        
        enemies = [zombie_close, skeleton_far, enderman, wither]
        other_maids = [maid_sword]  # 弓メイドさんから見た他メイドさん
        
        # 弓メイドさんの優先度分析
        print(f"\n【{maid_bow.name}の分析】")
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids)
        
        for enemy in enemies:
            distance = enemy.position.distance_to(maid_bow.position)
            print(f"  {enemy.name}: 優先度={bow_priorities[enemy]:.1f}, 距離={distance:.1f}")
        
        highest_bow = max(bow_priorities, key=bow_priorities.get)
        print(f"  → 最優先ターゲット: {highest_bow.name}")
        
        # 剣メイドさんの優先度分析
        print(f"\n【{maid_sword.name}の分析】")
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, [maid_bow])
        
        for enemy in enemies:
            distance = enemy.position.distance_to(maid_sword.position)
            print(f"  {enemy.name}: 優先度={sword_priorities[enemy]:.1f}, 距離={distance:.1f}")
        
        highest_sword = max(sword_priorities, key=sword_priorities.get)
        print(f"  → 最優先ターゲット: {highest_sword.name}")
        
        # 検証: 攻撃者が最優先であることを確認
        assert highest_bow == skeleton_far, "弓メイドさんもご主人を攻撃したスケルトンを最優先すべき"
        assert highest_sword == skeleton_far, "剣メイドさんもご主人を攻撃したスケルトンを最優先すべき"
        
        # 弓はエンダーマンを避けることを確認
        assert bow_priorities[enderman] < bow_priorities[zombie_close], "弓はエンダーマンよりゾンビを優先すべき"
    
    def test_weapon_compatibility_details(self):
        """武器相性の詳細テスト"""
        print("\n=== 武器相性詳細分析 ===")
        
        master = Master(Position(0, 0))
        maid_bow = Maid("弓メイドさん", "bow", Position(0, 0))
        maid_sword = Maid("剣メイドさん", "sword", Position(0, 0))
        
        # 近距離と遠距離の敵
        enemy_close = Enemy("近い敵", "zombie", Position(2, 0))  # 2ブロック
        enemy_far = Enemy("遠い敵", "skeleton", Position(6, 0))   # 6ブロック
        enderman = Enemy("エンダーマン", "enderman", Position(4, 0))  # 4ブロック
        
        enemies = [enemy_close, enemy_far, enderman]
        other_maids = []
        
        print("\n【弓メイドさんの武器相性】")
        bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids)
        for enemy in enemies:
            distance = enemy.position.distance_to(maid_bow.position)
            print(f"  {enemy.name} (距離{distance}): 優先度={bow_priorities[enemy]:.1f}")
        
        print("\n【剣メイドさんの武器相性】")
        sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids)
        for enemy in enemies:
            distance = enemy.position.distance_to(maid_sword.position)
            print(f"  {enemy.name} (距離{distance}): 優先度={sword_priorities[enemy]:.1f}")
        
        # 検証
        assert bow_priorities[enderman] == 0, "弓はエンダーマンを完全回避"
        assert sword_priorities[enderman] > 0, "剣はエンダーマンをターゲット可能"
    
    def test_dangerous_enemy_behavior(self):
        """危険敵への行動パターンテスト"""
        print("\n=== 危険敵行動パターン分析 ===")
        
        master = Master(Position(0, 0))
        maid = Maid("テストメイドさん", "sword", Position(2, 2))
        
        # 危険敵（ウィザー）を様々な状態で配置
        wither_passive = Enemy("待機ウィザー", "wither", Position(5, 5), is_dangerous=True)
        wither_attacking_master = Enemy("攻撃ウィザー", "wither", Position(5, 5), is_dangerous=True)
        wither_attacking_maid = Enemy("ターゲットウィザー", "wither", Position(5, 5), is_dangerous=True)
        
        # 攻撃設定
        wither_attacking_master.set_target(master)
        wither_attacking_maid.set_target(maid)
        
        # それぞれの状況での優先度を分析
        scenarios = [
            ("待機状態", [wither_passive]),
            ("ご主人攻撃中", [wither_attacking_master]),
            ("メイドさん攻撃中", [wither_attacking_maid])
        ]
        
        for scenario_name, enemies in scenarios:
            print(f"\n【{scenario_name}のウィザー】")
            priorities = calculate_enemy_priorities(maid, enemies, master, [])
            
            for enemy in enemies:
                distance = enemy.position.distance_to(maid.position)
                is_targeting_maid = enemy.is_targeting(maid)
                is_targeting_master = enemy.is_targeting(master)
                
                print(f"  優先度: {priorities[enemy]:.1f}")
                print(f"  距離: {distance:.1f}")
                print(f"  メイドさんをターゲット: {is_targeting_maid}")
                print(f"  ご主人をターゲット: {is_targeting_master}")
        
        # 検証: 危険敵の行動パターン
        passive_priority = calculate_enemy_priorities(maid, [wither_passive], master, [])
        attacking_master_priority = calculate_enemy_priorities(maid, [wither_attacking_master], master, [])
        attacking_maid_priority = calculate_enemy_priorities(maid, [wither_attacking_maid], master, [])
        
        assert passive_priority[wither_passive] <= 50, "待機ウィザーは低優先度"
        assert attacking_master_priority[wither_attacking_master] > passive_priority[wither_passive], "ご主人攻撃中は優先度上昇"
        assert attacking_maid_priority[wither_attacking_maid] <= 0, "メイドさんターゲット時は避難優先"
    
    def test_distribution_algorithm_details(self):
        """分散アルゴリズムの詳細テスト"""
        print("\n=== 分散アルゴリズム詳細分析 ===")
        
        master = Master(Position(0, 0))
        
        # 3体のメイドさん
        maid_a = Maid("メイドさんA", "sword", Position(1, 1))
        maid_b = Maid("メイドさんB", "sword", Position(-1, 1))
        maid_c = Maid("メイドさんC", "sword", Position(0, 2))
        
        # 同じ距離にある3体の敵
        zombie1 = Enemy("ゾンビ1", "zombie", Position(1, 0))
        zombie2 = Enemy("ゾンビ2", "zombie", Position(-1, 0))
        zombie3 = Enemy("ゾンビ3", "zombie", Position(0, -1))
        
        enemies = [zombie1, zombie2, zombie3]
        
        # シナリオ1: 誰もターゲットしていない状態
        print("\n【シナリオ1: 分散前】")
        other_maids = [maid_b, maid_c]
        priorities_a = calculate_enemy_priorities(maid_a, enemies, master, other_maids)
        
        for enemy in enemies:
            distance = enemy.position.distance_to(maid_a.position)
            print(f"  {enemy.name}: 優先度={priorities_a[enemy]:.1f}, 距離={distance:.1f}")
        
        # シナリオ2: 他メイドさんが集中している状態
        print("\n【シナリオ2: 集中攻撃後】")
        maid_b.set_target(zombie1)
        maid_c.set_target(zombie1)
        
        priorities_a_distributed = calculate_enemy_priorities(maid_a, enemies, master, other_maids)
        
        for enemy in enemies:
            targeting_count = sum(1 for maid in other_maids if maid.current_target == enemy)
            print(f"  {enemy.name}: 優先度={priorities_a_distributed[enemy]:.1f}, ターゲット数={targeting_count}")
        
        # 検証: 集中攻撃の回避
        assert priorities_a_distributed[zombie1] < priorities_a_distributed[zombie2], "集中ターゲットは優先度が下がる"
        assert priorities_a_distributed[zombie1] < priorities_a_distributed[zombie3], "集中ターゲットは優先度が下がる"


if __name__ == "__main__":
    pytest.main([__file__, "-s"])  # -s フラグで print 出力を表示