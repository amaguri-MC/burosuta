# 指示書 NB: ニタ（シロクマ）とバーリー（ウィッチ）を追加、全キャラの数値を本家に合わせる
先に agent/tasks/COMMON.md と CONTRACT.md（末尾「第3弾」）を読む。
担当範囲: src/main/java/com/amaguri/brawl/brawler/**, src/main/java/com/amaguri/brawl/core/**, src/main/java/com/amaguri/brawl/bot/BrawlBot.java, src/client/java/com/amaguri/brawl/client/hud/HudRenderer.java の頭上バーの色分け（team=2 を青）だけ, src/client/resources/assets/brawl/models/brawler/nitabear.json と textures/entity/brawler/nitabear.png, agent/lang/NB.json, agent/preview/NB/**
1. 数値の確認: 本家ブロスタの パワー1（またはレベル1）の数値を WebSearch / WebFetch（Brawl Stars の fandom wiki など）で調べ、7キャラ（シェリー=rusty, コルト=kaden, ブル=gouen, ブロック=mimosa, エルプリモ=takomaru, ニタ=nita, バーリー=barley）の HP・速さ・射程・ダメージ・弾数・リロード・必殺の溜まり方・必殺の性能を BrawlerStats に反映。出典 URL を agent/preview/NB/stats.md に表で残す（調べられなかった数値は今の値のまま印を付ける）。ゲーム内の名前は CONTRACT 第3弾の「モブ名（役割）」に（nameJa / 言語ファイル）。技名もモブらしく付け直す（例: 襲撃者=クロスボウ散弾/…、ウィッチ=毒のポーション/ポーションの雨 等）
2. ニタ（nita、シロクマ（クマ使い））: 通常=前へまっすぐ進む衝撃波（貫通、本家の幅と射程）。必殺=相棒のクマ（nitabear）を狙った位置に呼ぶ。クマは BrawlBot を使い Combat.join(bear, "nitabear")、HP・攻撃・速さは本家のクマ。召喚主が分かるようにし（BrawlState に ownerId を足す等）、召喚主と相棒は互いに当たらない・相棒の攻撃のダメージは召喚主の手柄（ゲージは溜めなくてよい）。相棒は一番近い見える敵を追って噛みつく思考（BrawlBot に分岐を足す）。召喚主が倒れたら相棒も消える。2体目を呼んだら前の相棒は消える。相棒は試合の残り人数に数えない（mode の Showdown が脱落処理で相棒を無視するか確認し、必要なら mode にも最小の修正をして返答に書く）。BarsPayload は召喚主から見て team=2
3. バーリー（barley、ウィッチ（投てき））: 通常=放物線で飛ぶポーション（壁を越える）→着地点に毒の水たまり（本家の半径・時間・毎秒ダメージ）。必殺=複数のポーションを前方の広い範囲に投げる。水たまりは緑の泡のパーティクル（この描画は今はパーティクルでよい）
4. 相棒のクマのモデル: tools/MODEL_FORMAT.md の形式で「鎧を着た大きなシロクマ」（全高 約30px、四つ足、背中と頭に鉄の鎧、前足が太い）を作り、tools/model_preview.py で確認（agent/preview/NB/bear_preview.png を Read）。四つ足だが固定ボーン名（right_arm=右前足, left_arm=左前足, right_leg=右後足, left_leg=左後足, head, body, weapon は空でよい）で歩きが付くように
5. ボット: 新キャラも BrawlBot が普通に使えること（ランダムに選ばれる対象に入る）
合格基準:
- [ ] stats.md に7キャラの出典つき数値表があり、BrawlerStats と一致
- [ ] ニタの相棒が「数に入らない・味方に当たらない・召喚主と一緒に消える」をコードで確認
- [ ] bear_preview.png を確認済み、警告なし
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内（mode に触れた場合はその箇所も）
