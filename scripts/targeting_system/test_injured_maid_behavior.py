"""
負傷メイドさんの挙動詳細テスト

ユーザー指摘の問題：
- 負傷メイドさんがターゲットしている敵に複数のメイドさんが集中してしまう
"""

from targeting_system import Position, Maid, Enemy, Master, calculate_enemy_priorities


def test_injured_maid_support_detailed():
    """負傷メイドさん支援の詳細動作確認"""
    
    print("\n=== 負傷メイドさん支援の詳細動作確認 ===")
    
    # 状況設定
    master = Master(Position(0, 0))
    maid_injured = Maid("MaidInjured", "sword", Position(5, 5), health=3)  # 重傷
    maid_support1 = Maid("MaidSupport1", "sword", Position(3, 3), health=20)
    maid_support2 = Maid("MaidSupport2", "bow", Position(2, 2), health=20)
    maid_support3 = Maid("MaidSupport3", "sword", Position(1, 1), health=20)
    
    # 複数の敵
    zombie1 = Enemy("Zombie1", "zombie", Position(6, 5))  # 負傷メイドさんに近い
    zombie2 = Enemy("Zombie2", "zombie", Position(1, 2))  # 他の場所
    skeleton = Enemy("Skeleton", "skeleton", Position(8, 8))  # 遠い場所
    
    # 負傷メイドさんがzombie1をターゲット
    maid_injured.set_target(zombie1)
    
    print(f"負傷メイドさん体力: {maid_injured.health}")
    print(f"負傷メイドさんターゲット: {maid_injured.current_target.name}")
    
    # 各メイドさんの優先度計算
    enemies = [zombie1, zombie2, skeleton]
    
    # Support1の優先度（他に負傷メイドさんのみ）
    other_maids_1 = [maid_injured, maid_support2, maid_support3]
    priorities_1 = calculate_enemy_priorities(maid_support1, enemies, master, other_maids_1)
    
    # Support2の優先度（Support1も考慮）
    maid_support1.set_target(zombie1)  # Support1が既にzombie1をターゲット
    other_maids_2 = [maid_injured, maid_support1, maid_support3]
    priorities_2 = calculate_enemy_priorities(maid_support2, enemies, master, other_maids_2)
    
    # Support3の優先度（Support1, Support2も考慮）
    maid_support2.set_target(zombie1)  # Support2もzombie1をターゲット
    other_maids_3 = [maid_injured, maid_support1, maid_support2]
    priorities_3 = calculate_enemy_priorities(maid_support3, enemies, master, other_maids_3)
    
    print("\n【各メイドさんの優先度】")
    print(f"Support1: Zombie1={priorities_1[zombie1]:.1f}, Zombie2={priorities_1[zombie2]:.1f}, Skeleton={priorities_1[skeleton]:.1f}")
    print(f"Support2: Zombie1={priorities_2[zombie1]:.1f}, Zombie2={priorities_2[zombie2]:.1f}, Skeleton={priorities_2[skeleton]:.1f}")
    print(f"Support3: Zombie1={priorities_3[zombie1]:.1f}, Zombie2={priorities_3[zombie2]:.1f}, Skeleton={priorities_3[skeleton]:.1f}")
    
    # 各メイドさんの最優先ターゲット
    target_1 = max(priorities_1, key=priorities_1.get)
    target_2 = max(priorities_2, key=priorities_2.get)  
    target_3 = max(priorities_3, key=priorities_3.get)
    
    print(f"\n【各メイドさんの最優先ターゲット】")
    print(f"Support1: {target_1.name}")
    print(f"Support2: {target_2.name}")
    print(f"Support3: {target_3.name}")
    
    # 集中の確認
    zombie1_targeting_count = [target_1, target_2, target_3].count(zombie1)
    print(f"\nZombie1をターゲットするメイドさん数: {zombie1_targeting_count}")
    
    # 期待される挙動：
    # 1体目：負傷メイドさん支援で軽いペナルティ
    # 2体目：過剰支援で重いペナルティ、別ターゲットを選ぶべき
    # 3体目：さらに別ターゲットを選ぶべき
    
    if zombie1_targeting_count <= 2:
        print("✅ 適切な分散：過剰集中を回避")
    else:
        print("❌ 過剰集中：3体全てがZombie1をターゲット")
    
    return zombie1_targeting_count <= 2


def test_injured_maid_priority_comparison():
    """負傷メイドさん支援時の優先度詳細比較"""
    
    print("\n=== 負傷メイドさん支援時の優先度詳細比較 ===")
    
    master = Master(Position(0, 0))
    maid_injured = Maid("MaidInjured", "sword", Position(2, 2), health=5)
    maid_supporter = Maid("MaidSupporter", "sword", Position(1, 1), health=20)
    
    zombie_injured_target = Enemy("ZombieInjuredTarget", "zombie", Position(3, 2))
    zombie_normal = Enemy("ZombieNormal", "zombie", Position(3, 1))
    
    # Case 1: 負傷メイドさんがターゲット中、支援者なし
    maid_injured.set_target(zombie_injured_target)
    other_maids_case1 = [maid_injured]
    priorities_case1 = calculate_enemy_priorities(maid_supporter, [zombie_injured_target, zombie_normal], master, other_maids_case1)
    
    # Case 2: 負傷メイドさんがターゲット中、既に1体が支援中
    maid_supporter_existing = Maid("ExistingSupporter", "sword", Position(0, 2), health=20)
    maid_supporter_existing.set_target(zombie_injured_target)
    other_maids_case2 = [maid_injured, maid_supporter_existing]
    priorities_case2 = calculate_enemy_priorities(maid_supporter, [zombie_injured_target, zombie_normal], master, other_maids_case2)
    
    # Case 3: 負傷メイドさんがターゲット中、既に2体が支援中
    maid_supporter_existing2 = Maid("ExistingSupporter2", "bow", Position(-1, 2), health=20)
    maid_supporter_existing2.set_target(zombie_injured_target)
    other_maids_case3 = [maid_injured, maid_supporter_existing, maid_supporter_existing2]
    priorities_case3 = calculate_enemy_priorities(maid_supporter, [zombie_injured_target, zombie_normal], master, other_maids_case3)
    
    print("【負傷メイドさん支援優先度の変化】")
    print(f"Case1(支援者なし): 負傷ターゲット={priorities_case1[zombie_injured_target]:.1f}, 通常敵={priorities_case1[zombie_normal]:.1f}")
    print(f"Case2(支援者1体): 負傷ターゲット={priorities_case2[zombie_injured_target]:.1f}, 通常敵={priorities_case2[zombie_normal]:.1f}")
    print(f"Case3(支援者2体): 負傷ターゲット={priorities_case3[zombie_injured_target]:.1f}, 通常敵={priorities_case3[zombie_normal]:.1f}")
    
    # ターゲット選択の変化を確認
    target_case1 = max(priorities_case1, key=priorities_case1.get)
    target_case2 = max(priorities_case2, key=priorities_case2.get)
    target_case3 = max(priorities_case3, key=priorities_case3.get)
    
    print(f"\n【ターゲット選択の変化】")
    print(f"Case1: {target_case1.name}")
    print(f"Case2: {target_case2.name}")  
    print(f"Case3: {target_case3.name}")
    
    # 期待される挙動
    expected_case1 = target_case1 == zombie_injured_target  # 支援すべき
    expected_case2 = target_case2 == zombie_injured_target  # まだ支援可能（軽いペナルティ）
    expected_case3 = target_case3 == zombie_normal  # 過剰支援回避で別ターゲット
    
    print(f"\n【期待通りの挙動】")
    print(f"Case1(負傷支援): {'✅' if expected_case1 else '❌'}")
    print(f"Case2(継続支援): {'✅' if expected_case2 else '❌'}")
    print(f"Case3(過剰回避): {'✅' if expected_case3 else '❌'}")
    
    return expected_case1 and expected_case2 and expected_case3


if __name__ == "__main__":
    test1_result = test_injured_maid_support_detailed()
    test2_result = test_injured_maid_priority_comparison()
    
    print(f"\n=== 総合結果 ===")
    print(f"分散テスト: {'✅ PASS' if test1_result else '❌ FAIL'}")
    print(f"優先度変化テスト: {'✅ PASS' if test2_result else '❌ FAIL'}")
    
    if test1_result and test2_result:
        print("✅ 負傷メイドさん支援の挙動は適切")
    else:
        print("❌ 負傷メイドさん支援に問題あり、調整が必要")