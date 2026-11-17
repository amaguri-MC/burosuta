# 指示書 W3: arena（ブロック・マップ図・建築）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT.md「W3 arena」を実装し、ブロスタのバトルロイヤル用マップを1枚作る。
担当範囲: src/main/java/com/amaguri/brawl/arena/**, src/main/resources/assets/brawl/{blockstates,models/block,textures/block,items}/ のうちブロックとPOWER_CUBE分, src/main/resources/data/brawl/**, agent/lang/W3.json, agent/preview/W3/**
成果物: ブロック7種＋POWER_CUBE アイテム、data/brawl/maps/showdown.txt（50×50文字）、ArenaMap/Arena、agent/preview/W3/map.png（マップの真上図。1タイル=8px、色分け）と blocks.png（各テクスチャ8倍）
マップ設計:
- ブロスタのソロ用マップ風（オリジナル配置でよい）。外周は `=`。スタート `S` 10個は外周寄りにほぼ等間隔。中央は開けて箱が多め。茂みは帯状・かたまりで点在（全体の15〜20%）。壁は L字・短い列で遮蔽を作る。箱 `X` 14〜18個。ロープ柵 `R` 少し
- 回転対称（4回）を基本に少し崩す。全スタートと中央が歩いてつながっていること（BFS で確認するスクリプトを agent/preview/W3/check.py に）
見た目: 床は2色のチェック（タイルごと交互、明るい緑と少し暗い緑）。壁=明るい土色の岩ブロック、外壁=暗めの岩、茂み=濃い緑の葉（cutout、当たり判定なし・通れる）、箱=木箱に緑の光る模様、ロープ=低い杭とロープ（当たり判定1.5高さ、見た目は細い）
制約:
- build は毎回 範囲をいったん空気で消してから建てる（前の試合の壊れた壁を戻す）。床の下は石で埋める。チャンクの読み込みに注意（setBlock 前に必要ならチャンクを読む）
- 茂みは光を通し、パーティクルや弾を止めない
合格基準:
- [ ] CONTRACT の W3 署名が一字一句そのまま存在
- [ ] check.py で全スタート→中央が到達可能、S=10、X 14〜18 を出力
- [ ] map.png と blocks.png を自分で Read して見た目を確認した
- [ ] 26.2 API と blockstate/model JSON 形式が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
