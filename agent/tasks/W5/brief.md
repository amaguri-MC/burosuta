# 指示書 W5: bot（ボットの実体・AI・描画）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT.md「W5 bot」を実装。人数が足りない枠を埋め、ちゃんと遊び相手になるボット。
担当範囲: src/main/java/com/amaguri/brawl/bot/**, src/client/java/com/amaguri/brawl/client/bot/**, brawl.bot.mixins.json, brawl.botclient.mixins.json, agent/lang/W5.json
手本: /home/claude/ref-aibot の entity/FakeMember と client/FakeMemberRenderer・mixin/EntityRenderDispatcherMixin（26.2 で AvatarRenderState 系を使うと自作 Renderer が呼ばれない問題の対策が入っている）
制約:
- 見た目: プレイヤーモデル＋バニラ既定スキン（Steve/Alex など既定の数種からランダム）、手に Brawlers.weaponItem(id)、名前タグはボット名。ブロスタのキャラの見た目は作らない
- spawn で Combat.join(bot, brawlerId) を呼ぶ。速さは BrawlState/def から speedFor で。バニラのAIゴールは使わず自前の思考（毎tick）
- 思考（優先順）: ①毒ガスの中/5秒以内に飲まれる → 安全地帯の中心へ ②HP35%未満で敵が近い → 離れて茂みへ（回復を待つ） ③射程×1.2以内に見える敵（Combat.isHiddenFrom で隠れていない）→ 射程に入るよう近づき、左右に動きながら Brawlers.attack（狙いのずれ: easy ±15° normal ±7° hard ±3°、反応 easy 0.8秒 normal 0.4秒 hard 0.2秒）。ゲージ満タンで useSuper ④近くのキューブ（ArenaBlocks.POWER_CUBE の ItemEntity）を拾いに行く ⑤一番近い箱を壊しに行く ⑥中央寄りに歩く
- 移動は navigation.moveTo（経路の再計算は1秒に1回まで）。詰まったら別の目標へ
- ボットは保存しない（ワールド保存時に消える設定）
合格基準:
- [ ] CONTRACT の W5 署名が一字一句そのまま存在
- [ ] 隠れている敵を狙わない、毒ガスから逃げる、が思考コードで確認できる
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
