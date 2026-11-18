# 指示書 R2: モブの見た目の不具合修正と新キャラ（クライアント側）
先に agent/tasks/COMMON.md、CONTRACT.md（末尾「第3弾」）を読む。client/model/MobPuppets.java など（前任 MOB の作）を読んで直す。
担当範囲: src/client/java/com/amaguri/brawl/client/model/**, src/client/java/com/amaguri/brawl/client/hud/Faces.java, src/client/java/com/amaguri/brawl/client/select/**, src/client/java/com/amaguri/brawl/client/hud/HudClient.java と HudState.java の StatusPayload 受信の追加だけ, src/client/java/com/amaguri/brawl/client/world/**（状態異常の見た目を描く場合）
1. 不具合: ボット（BrawlBot）の体が描かれない（頭上の名前とHPバーだけ出る）。プレイヤーは人形で描けている。実機スクショ: /home/claude/v4.png 左上（ロビーで7体並べたのに体が無い）。BotRenderer / BotRenderState / client/bot/mixin/EntityRenderDispatcherMixin（W5作）と client/model/mixin/BrawlerRenderDispatcherMixin（優先度500）の振り分けを読み、ボットでも人形が描かれるよう直す（人形が作れないときに元のプレイヤー型ボットの見た目に戻ることも確認）
2. 対応表の更新: gouen→COW, frank→IRON_GOLEM, darryl→COPPER_GOLEM, poco→ALLAY, mortis→WANDERING_TRADER, edgar→WITHER_SKELETON, lou→SNOW_GOLEM, rusty→PILLAGER, mimosa→BLAZE, nita→POLAR_BEAR, barley→WITCH, takomaru→（保留: 今は VINDICATOR）。kaden（スケルトン）は削除。アレイは小さく浮くので高さ1.9にそろえるときに地面から少し浮かせる。スノーゴーレムはカボチャ頭あり。顔アイコン（Faces）も新しいモブに（頭の正面UVはバニラの知識で、UNVERIFIED 付き）
3. 状態異常の見た目（StatusPayload を受けて HudState に保持）: 凍結=人形を水色に（色付け or 氷のブロックを重ねて描く）、スタン=頭上に回る星（小さな黄色の点でよい）、減速=足元に青い輪、凍結ゲージ>0 は頭上バーの下に水色の細いバー（HudRenderer は U 作。頭上表示に1本足す変更は可）
4. 自分の画面: スタン・凍結中は画面の縁を水色に
合格基準:
- [ ] ボットも人形で描かれる理由をコードで説明できる（返答に1行）
- [ ] 11キャラ分の対応表と顔
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
