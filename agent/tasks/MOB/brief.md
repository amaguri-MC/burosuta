# 指示書 MOB: キャラの見た目をバニラのモブにする
先に agent/tasks/COMMON.md と CONTRACT.md（末尾「第3弾」）を読む。
目的: 試合・ロビーの参加者（プレイヤー・ボット）と像を、選んだキャラに対応するバニラのモブの見た目で描く（MC 26.2 Fabric クライアント）。
担当範囲: src/client/java/com/amaguri/brawl/client/model/**, src/client/java/com/amaguri/brawl/client/hud/Faces.java, src/client/java/com/amaguri/brawl/client/select/**, agent/lang/MOB.json
方式（推奨）: 「人形」方式。BrawlerLooks（実体ID→キャラID）に載った実体を描くとき、その実体ごとにクライアント専用のバニラのモブ実体（EntityTypes.PILLAGER など。level に追加しない仮の実体、setId は負の値）を持ち、毎フレーム 位置・向き(yRot/yBodyRot/yHeadRot/xRot)・歩きアニメ（walkAnimation）・攻撃の振り（swingTime/attackAnim）・tickCount を本体から写して、EntityRenderDispatcher で人形を描く（本体の代わりに）。今の自作モデルの振り分け（BrawlerRenderDispatcherMixin）を拡張する形がよい
- 高さを揃える: 人形のバニラの寸法（EntityType.getHeight など）から倍率を出し、見た目の高さ約1.9ブロックになるよう縮小/拡大（ホグリン・アイアンゴーレム・シロクマは縮む）。幅が広すぎる場合も幅1.2ブロック程度に収まるように
- 持ち物: 襲撃者=クロスボウ、スケルトン=弓、ウィッチは攻撃した直後だけポーションを持つ。ホグリン・ブレイズ・ゴーレム・シロクマは無し
- 攻撃の演出: サーバーは攻撃時に swing を送る → 人形にも反映（アイアンゴーレムは腕振り、ホグリンは頭突き等、そのモブのアニメが自然に出る方法を選ぶ。難しければ swing だけでよい）
- ブレイズは浮いているので、人形を地面から少し浮かせてよい
- `nitabear`（ニタの相棒）は自作モデル（assets/brawl/models/brawler/nitabear.json があれば今の自作モデル描画）。表に無い id は今の自作モデル→無ければ通常の見た目
- 赤く光る被弾演出（hurtTime）も写す。茂みで隠れた実体（HiddenPayload）は描かない（今の仕組みを壊さない）
- 顔アイコン（Faces）: HUD・キルログ・選択画面の顔をモブの顔に。バニラのテクスチャ（assets/minecraft/textures/entity/...）を ResourceManager から読んで頭の正面の UV を切り出す（各モブの頭の正面 UV は、そのモデルクラスの createBodyLayer の texOffs と箱の寸法から求める。apidump の client.txt に createBodyLayer はあるが数値は無いので、バニラの知識で決め、`// UNVERIFIED:` を付ける）。シロクマ・ブレイズ等の顔が切り出しにくければ、人形を GUI に小さく描く方式でもよい
- キャラ選択画面の3D表示も人形で
- 26.2 API は apidump（client.txt / common.txt）で実在確認。過去の学び: GUI に描くだけの実体は setId(負の仮ID) 必須、26.2 は EntityTypes.PILLAGER（EntityType.PILLAGER ではない）、mc.gui.setScreen
合格基準:
- [ ] 7キャラすべてがモブの見た目で、高さがそろう
- [ ] 人形が本体と同じ位置・向きで、歩き・攻撃のアニメが出る
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
