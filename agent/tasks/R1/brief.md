# 指示書 R1: キャラ追加と入れ替え（サーバー側）
先に agent/tasks/COMMON.md、CONTRACT.md（末尾「第3弾」）、agent/tasks/R1/stats.md を読む。数値は stats.md が正（stats.md の値を BrawlerStats に）。
担当範囲: src/main/java/com/amaguri/brawl/brawler/**, src/main/java/com/amaguri/brawl/core/**, src/main/java/com/amaguri/brawl/bot/BrawlBot.java（新キャラを使う思考の調整だけ）, src/main/resources/assets/brawl/{items,models/item}/<新id>_weapon.json（テクスチャは既存のものを流用してよい）, agent/lang/R1.json
やること:
1. kaden（コルト）を削除（定義・アイテム・ボットの候補から）。takomaru は保留（今のまま残す）。gouen の名前は「ウシ（タンク）」
2. 新キャラ6人: frank, darryl, poco, mortis, edgar, lou（名前・クラス・技名は stats.md の対応で、技名はモブらしく付ける）
3. 新しい仕組み（core に作る）:
   - 状態異常: スタン（移動・攻撃・必殺 不可）、凍結（同じ）、減速（速さ×0.6 など）。プレイヤーにもボットにも効く（プレイヤーは移動速度の属性を0付近＋毎tick 横移動を止める、攻撃は Brawlers.attack が拒否）。`Combat.stun(e, ticks)`, `Combat.freeze(e, ticks)`, `Combat.addFrost(e, pct)`（100で凍結）, `Combat.slow(e, ticks, mul)`
   - 自動の必殺チャージ（darryl・edgar）
   - 回復（ライフスティール）: 与えたダメージの割合で回復（edgar）、命中ごとに固定回復（mortis 必殺）
   - 溜め攻撃（frank: 1.3秒動けず、その後に発射）
   - 突進（mortis 通常・darryl 必殺・gouen 必殺）は今の gouen の突進処理を共通化
   - 氷の床（lou 必殺）: 範囲と残り時間をサーバーで持ち、その上の敵の向き変化を見て減速／一時停止。床の見た目は白〜水色のパーティクルでよい（後で専用描画にする）
   - 回復の扇（poco 必殺）: ソロでは自分を回復（味方の概念がある召喚物＝ニタのクマも回復してよい）
   - StatusPayload（api/Net.java に司令塔が追加済み）を 2tick ごとに送る（受け手の48ブロック以内で状態がある実体だけ）
4. ボット: 新キャラを使えるように（frank は溜め中に動かない、edgar は必殺で敵の近くへ跳ぶ、mortis は突進で詰める、darryl は必殺で突っ込む、lou は凍った敵を優先 等、最低限）
5. ロビーの像が7体以上になるので、lobby/LobbyEntities の像の数が Brawlers.all() に従うか確認し、固定数なら返答に書く（lobby は担当外。触らない）
合格基準:
- [ ] stats.md と BrawlerStats が一致（推定値は推定のまま）
- [ ] スタン・凍結・減速がプレイヤーとボットの両方で効く（コードで追う）
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
