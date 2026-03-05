# CookingMode/PharmacistMode を Strategy パターンで再設計

## ステータス

承認済み

## コンテキスト

CookingMode（かまど操作）と PharmcistMode（醸造台操作）は、ブロック探索・移動・しゃがみ・排他制御・NBT保存・音再生といった共通ロジックが丸コピーされていた。約980行が2ファイルに重複しており、以下の問題があった:

- **Copy-Paste Programming**: 共通ロジックの修正が2箇所に必要
- **SRP 違反**: 1クラスが探索・移動・排他制御・スロット操作・音再生をすべて担当
- **グローバル状態の分散**: 排他制御用の静的マップが各クラスに個別に存在し、ディメンション跨ぎの考慮もなかった
- **タイポ**: クラス名が `PharmcistMode`（a が欠落）

## 決定

継承（Template Method）ではなく **委譲（Strategy パターン）** で再設計した。

### 新しいクラス構成

| クラス | 責務 |
|-------|------|
| `BlockWorkMode` | Mode のライフサイクル管理。探索・移動・しゃがみ・NBT を担当。内部の `Delegate<T>` で型安全性を確保 |
| `WorkStrategy<T>` | ブロック固有の作業ロジックのインターフェース |
| `FurnaceWorkStrategy` | かまど固有のロジック（旧 CookingMode から抽出） |
| `BrewingWorkStrategy` | 醸造台固有のロジック（旧 PharmcistMode から抽出） |
| `BlockReservationManager` | 排他制御の一元管理。`GlobalPos` でディメンション対応 |
| `WorkActions` | 音・アニメーションのコールバックインターフェース |

### ジェネリクスの型安全性

`BlockWorkMode` 自体は非ジェネリクスだが、内部に `Delegate<T extends BlockEntity>` を持つことで、`@SuppressWarnings("unchecked")` なしに型安全を実現した。

## 根拠

### 継承（Template Method）を却下した理由

- ユーザーの好みとして継承より委譲を選好
- Java の単一継承制約により将来の拡張性が制限される
- Strategy は各コンポーネントを独立してテスト可能
- 共通ロジックと固有ロジックの境界が明確

### BlockReservationManager を外出しした理由

- 排他制御のグローバル状態が各モードクラスに分散していた
- `GlobalPos` を使うことでディメンション跨ぎの問題を解消
- 将来のブロック操作モード追加時に再利用可能

### Delegate パターンを採用した理由

`BlockWorkMode` を非ジェネリクスにしつつ内部で型安全を保つため。`ModeType<BlockWorkMode>` とシンプルに書ける一方、内部の `Delegate<T>` がワイルドカードキャプチャの問題を回避する。

## 影響

- `CookingMode.java`（412行）と `PharmcistMode.java`（567行）を削除
- 6つの新ファイルに責務を分離（合計行数は同程度だが重複なし）
- `Modes.java` のフィールド型が `ModeType<CookingMode>` → `ModeType<BlockWorkMode>` に変更
- 新しいブロック操作モードは `WorkStrategy<T>` を実装するだけで追加可能
