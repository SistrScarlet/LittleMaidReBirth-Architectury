# 1.21.1 で再ログイン時に先制攻撃タグが全消失する回帰

## 背景

1.21.1 移植後、全 EntityType が先制攻撃対象になる不具合が報告された。IFF 画面のアイコンが全て剣になり、
メイドさんが鶏を襲うことで発覚した。ワールドを再読み込みすると発生し、セーブ&ロードでは発生しない。

## 症状

`TargetTagManagerImpl` の `targetTagMap` が「85 キーは存在するが値の Set が全て空」という状態になる。

`getTargetTag` はキーが存在しない場合のみ安全側の `PREEMPTIVE_ATTACK_PROHIBITED` を返すため、
「キーはあるが空」だと先制攻撃禁止が効かず、全モブが攻撃対象になる。

さらに `init()` のマージ順序が

```java
var tmp = new HashMap<>(TARGET_TAG_MAP);  // デフォルト
tmp.putAll(targetTagMap);                 // ロード値がデフォルトを上書き
this.targetTagMap.putAll(tmp);
```

であるため、一度「85 キー全空」が保存されると以後デフォルトに戻らず、自然回復しない。

## 調査経緯

デバッグビルドを重ねて以下の順に切り分けた。

| 検証 | 結果 |
|---|---|
| `init()` のデフォルト生成 | 正常（85 件、chicken に `PREEMPTIVE_ATTACK_PROHIBITED`） |
| `TargetTagManagerImpl` の 1.20.1 との差分 | 差分なし |
| メモリ上の write 内容 | 正常（`tagsNonEmpty=51`） |
| 書いた NBT をその場で読み直す roundtrip | 正常（51 件）→ シリアライズは無罪 |
| ディスク上の level.dat / playerdata | 正常（51 件）→ 保存経路は無罪 |
| 読み込みに渡された生 NBT | **空 85 件** → 読み込み経路が犯人 |
| メイドさん（同じ read/write コード） | 常に正常 |

保存経路はスタックトレースで確定した。

```
IntegratedPlayerManager.savePlayerData
  ├ userData = player.writeNbt(...)  → level.dat の Data.Player タグへ
  └ super.savePlayerData(...)        → playerdata/*.dat へ
```

`writeCustomDataToNbt` が毎回 2 回発火していたのはこの 2 経路のため。

## 原因

`NbtOps.createList` は**全要素が `NbtByte` の `NbtList` を `NbtByteArray` に変換する**
（`NbtOps$ByteArrayMerger`。`NbtInt` → `NbtIntArray`、`NbtLong` → `NbtLongArray` も同様）。

シングルプレイのホストプレイヤーは playerdata ではなく **level.dat の `Player` タグ**から読み込まれる
（`PlayerManager.loadPlayerData` がホスト名一致時に `saveProperties.getPlayerData()` を優先）。
1.21 ではこの経路が NBT →`Dynamic`→ NBT の往復を通るため、上記の変換を受ける。

一方 read 側は `getList("tags", NbtElement.BYTE_TYPE)` を使っていた。
`NbtCompound#getList(key, type)` は **型が `LIST` でなければ問答無用で空リストを返す**ため、
`BYTE_ARRAY` に化けた `tags` が全て空として読まれていた。

実測値:

```
ディスク:     tags = TAG_List(BYTE) [2]      ← 正しく保存されている
読み込み後:   tags = TAG_Byte_Array [2]      ← 値は無事だが型が違う
read():      getList(…BYTE_TYPE) → 空       ← ここで全消失
```

元から空だったタグ（34 件）が `LIST` のまま残っていたのも、要素が無く変換対象外だったため。
メイドさんが無傷だったのは、エンティティ NBT がこの往復を通らないため。

## 1.20.1 との比較

`NbtOps` の inner class 構成（`ByteArrayMerger` 等）は 1.20.1 と 1.21.1 で**完全同一**であり、
変換ロジック自体は以前から存在する。変わったのは level.dat の `Player` タグが `Dynamic` 往復を
通るようになった点。

1.20.1 環境の実セーブ（`fabric/run/saves/1_20_1/level.dat`）を直接ダンプして裏を取った。

| | `tags` の型 | 結果 |
|---|---|---|
| 1.20.1 の level.dat | 全 82 件が `LIST`（chicken = `LIST(BYTE)[2]`） | 保持されている |
| 1.21.1 の読み込み後 | 中身のある 51 件が `BYTE_ARRAY` | 全消失 |

**1.21.1 固有の回帰**であり、1.20.1 へのバックポートは不要。

## 修正

`TargetTagManagerImpl`:

- write: `putByteArray("tags", ordinals)` に統一（変換後の形にそろえ、往復で不変にする）
- read: `readTagOrdinals` を追加し、`BYTE_ARRAY` と旧 `NbtList` の**両形式**を受け付ける

旧形式の互換パスを残したので、1.20.1 のセーブを 1.21.1 で開いても正しく読める。

既に「85 キー全空」が保存されたワールドは自動回復しない。これは「ユーザーが意図的に全タグを外した」
状態と区別できないため、自動上書きは危険と判断した。該当ワールドは IFF を設定し直す必要がある。

## 横断調査

`NbtOps` が配列化するのは `NbtByte` / `NbtInt` / `NbtLong` のリストのみ。全プロジェクトを走査した結果、
`NbtByte.of` / `NbtInt.of` / `NbtLong.of` は他に一箇所も使われていなかった
（リストに数値を直接入れるにはこれらを使うしかないため、これで網羅できる）。

| 箇所 | 要素型 | 判定 |
|---|---|---|
| `TargetTagManagerImpl` | ByteArray | 修正済み |
| `MaidManagerImpl`（プレイヤー NBT） | `NbtCompound` | 安全 |
| `LMItemContractable` | `NbtCompound`（中身は `NbtIntArray`） | 安全 |
| `LMHasInventory` | `NbtCompound`（ItemStack） | 安全 |
| `WorldMaidSoulState` | `NbtCompound` | 安全 |
| LMML / TILM / LMRBCompat / ZabutonR | `NbtList` 生成なし | 該当なし |
| ActionArms（4 箇所） | 数値リストなし | 安全 |

## 結論・教訓

- **プレイヤー NBT に `NbtByte` / `NbtInt` / `NbtLong` のリストを保存してはいけない。**
  `Dynamic` 往復で配列型に変換され、`getList(key, TYPE)` が黙って空を返す。
  数値列は最初から `putByteArray` / `putIntArray` / `putLongArray` を使う。
- `NbtCompound#getList(key, type)` は型不一致で**例外を投げず空を返す**。
  サイレント失敗なので、読めているつもりで全損しても気付けない。
- 同じ read/write コードでもエンティティ NBT とプレイヤー NBT で挙動が変わりうる。
  シングルプレイのホストは playerdata ではなく level.dat の `Player` タグから読まれる点に注意。
- 「デフォルト値をロード値で上書きする」マージは、ロード値が壊れた際に自然回復を塞ぐ。
