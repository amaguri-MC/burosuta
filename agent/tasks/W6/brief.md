# 指示書 W6: hud（画面表示・入力・茂みで隠す・頭上HPバー）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT.md「W6 hud」を実装。ブロスタらしく読みやすい画面にする。
担当範囲: src/client/java/com/amaguri/brawl/client/hud/**, brawl.hud.mixins.json, src/main/resources/assets/brawl/textures/gui/**, agent/lang/W6.json, agent/preview/W6/**
画面（試合中だけ。HudPayload.brawler が "" のときは何も出さない。バニラのハート・空腹・経験値バーは隠す）:
- 下中央: HPの数字とバー（自分=緑）、その下に弾3本（溜まり途中も伸びる）
- 右下: 必殺技の丸ゲージ（満タンで光って「R」）
- 上中央: 残り人数（「残り 7」）、自分のキューブ数
- 毒ガス: 次の縮みまでの秒、ガスの中にいると画面の縁が緑に
- カウントダウン中は大きな数字、終了時は順位（#3 など）
- 頭上HPバー: BarsPayload の各実体の頭の上に HP数字＋バー（自分=緑、敵=赤）、キューブ数。隠れた実体は出さない
- 機能UIなので読みやすさ優先。角丸カードの羅列・中央のポップアップ・意味のない色タイルは避ける。太めの縁取り文字と細いバー
入力（試合中・生存中だけ）: 左クリック/右クリック → AttackPayload(プレイヤーのyaw)（押しっぱなしで連射。送る間隔は4tick以上）。バニラの攻撃・使用・ブロック破壊は止める。R（キー設定に「必殺技」として登録）→ SuperPayload
茂み: HiddenPayload の実体は描画しない（名前タグ・影も）
制約:
- 26.2 の HUD 描画（Fabric の HudElementRegistry 等）・ワールド内描画は apidump で実在を確認。頭上バーはワールド内描画が難しければ、実体の位置を画面座標に射影して HUD に描く方式でもよい
- agent/preview/W6/hud.png に画面配置の見本（1280x720、Python で描く）を作り自分で Read して確認
合格基準:
- [ ] 上の表示がすべてある、試合外では何も出ない
- [ ] 入力が試合中だけ効き、バニラの攻撃・破壊が止まる
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
