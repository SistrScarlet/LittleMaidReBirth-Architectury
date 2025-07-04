"""
シナリオ19のデバッグ: 武器特性閾値の境界テスト
"""

from targeting_system import Position, Maid, Enemy, Master, calculate_enemy_priorities

def debug_scenario19():
    print("=== シナリオ19デバッグ: 武器特性閾値の境界テスト ===")
    
    # 状況設定
    master = Master(Position(0, 0))
    maid_bow = Maid("MaidBow", "bow", Position(1, 1))
    maid_sword = Maid("MaidSword", "sword", Position(-1, 1))
    
    # 7ブロック先（近距離側）
    zombie1 = Enemy("Zombie1", "zombie", Position(7, 0))
    # 9ブロック先（遠距離側）
    zombie2 = Enemy("Zombie2", "zombie", Position(9, 0))
    
    enemies = [zombie1, zombie2]
    other_maids_bow = [maid_sword]
    other_maids_sword = [maid_bow]
    
    # 距離の確認
    bow_to_zombie1 = zombie1.position.distance_to(maid_bow.position)
    bow_to_zombie2 = zombie2.position.distance_to(maid_bow.position)
    sword_to_zombie1 = zombie1.position.distance_to(maid_sword.position)
    sword_to_zombie2 = zombie2.position.distance_to(maid_sword.position)
    
    print(f"弓からZombie1の距離: {bow_to_zombie1:.1f}")
    print(f"弓からZombie2の距離: {bow_to_zombie2:.1f}")
    print(f"剣からZombie1の距離: {sword_to_zombie1:.1f}")
    print(f"剣からZombie2の距離: {sword_to_zombie2:.1f}")
    
    # 優先度計算
    bow_priorities = calculate_enemy_priorities(maid_bow, enemies, master, other_maids_bow)
    sword_priorities = calculate_enemy_priorities(maid_sword, enemies, master, other_maids_sword)
    
    print(f"\n【弓メイドさんの優先度】")
    print(f"Zombie1: {bow_priorities[zombie1]:.1f}")
    print(f"Zombie2: {bow_priorities[zombie2]:.1f}")
    
    print(f"\n【剣メイドさんの優先度】")
    print(f"Zombie1: {sword_priorities[zombie1]:.1f}")
    print(f"Zombie2: {sword_priorities[zombie2]:.1f}")
    
    # 最優先ターゲット
    bow_target = max(bow_priorities, key=bow_priorities.get)
    sword_target = max(sword_priorities, key=sword_priorities.get)
    
    print(f"\n【最優先ターゲット】")
    print(f"弓: {bow_target.name}")
    print(f"剣: {sword_target.name}")
    
    # 期待と実際の比較
    expected_bow = zombie2  # 遠距離敵を期待
    expected_sword = zombie1  # 近距離敵を期待
    
    print(f"\n【期待vs実際】")
    print(f"弓の期待: {expected_bow.name}, 実際: {bow_target.name} ({'✅' if bow_target == expected_bow else '❌'})")
    print(f"剣の期待: {expected_sword.name}, 実際: {sword_target.name} ({'✅' if sword_target == expected_sword else '❌'})")
    
    return bow_target == expected_bow and sword_target == expected_sword

if __name__ == "__main__":
    result = debug_scenario19()
    print(f"\n総合結果: {'✅ SUCCESS' if result else '❌ FAILED'}")