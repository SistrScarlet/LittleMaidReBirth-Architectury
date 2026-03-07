# TODO

## 高

- [ ] LittleMaidEntity.java の分割リファクタ（2000行超、Checkstyle FileLength 違反）

## 中

- [ ] モード続行判定の拡張 — 無手でもモード続行すべき場合に対応（HasModeImpl/Mode に関数追加）。薬剤師モードでメインハンドの水瓶も醸造に使えるようにする
- [ ] docs/ ディレクトリへのドキュメント作成方針の決定

- [ ] 移動モード×お仕事モードの組み合わせ表示名（例: 護衛剣士, 自由剣士）— 旧バージョンにあった機能の復活

- [ ] 探索範囲・探索数などハードコードされた値のコンフィグ化（BlockWorkMode の SEARCH_DISTANCE, SEARCH_MAX_COUNT, SEARCH_BUDGET_PER_TICK 等）

## 低

- [ ] EQ_DOESNT_OVERRIDE_EQUALS（TargetingSystem.Mob/Maid）— 実害なしだが将来の安全のため検討
