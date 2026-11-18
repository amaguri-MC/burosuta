# 指示書 G: 毒ガスと射程ガイドを専用の描画に
先に agent/tasks/COMMON.md と CONTRACT.md を読む。
目的: パーティクルは遅れて付いてくるので、ワールド内に直接描く（MC 26.2 Fabric クライアント）。
担当範囲: src/client/java/com/amaguri/brawl/client/world/**（新規）, src/client/java/com/amaguri/brawl/client/view/TopDownClient.java の guide() 部分（パーティクルの照準ガイドを消す）, agent/lang/G.json（必要なら）。サーバー側の毒ガスの縁パーティクル（mode/Gas か Showdown）を止める1行は返答に書けば司令塔がやる
描画:
- 毒ガス: HudState.match の安全地帯の四角（gasMinX..gasMaxX, gasMinZ..gasMaxZ）の4辺に、床（アリーナ床の高さ。プレイヤーの足元付近）から高さ6ブロックの緑の半透明の壁（上ほど薄く）。外側の地面は緑の半透明で塗る（アリーナの範囲内だけ。範囲は gas の初期値＝最初に受け取った四角で覚える）。縮みは補間してなめらかに
- 射程ガイド: 見下ろし中（TopDown.active()）、プレイヤーの足元から aimYaw 方向へ射程（Brawlers.get(id).attackRangeTiles() × Tiles.BLOCKS_PER_TILE）の白い半透明の帯（幅 1.0 ブロック、先端は少し明るい）。毎フレーム（部分tick補間した位置と TopDown.aimYaw）で描くので遅れない。R を押している間は必殺の射程（superRangeTiles）で黄色
- 描画は Fabric の LevelRenderEvents（/home/claude/apidump/fabric/fabric.txt の net.fabricmc.fabric.api.client.rendering.v1.level.*）の半透明の後のタイミングで。頂点の出し方（RenderType / RenderPipeline / BufferBuilder 等）は 26.2 の apidump で実在確認し、手本が要れば Fabric の 26.2 ブランチの testmod（/home/claude/fabric-26.2 があれば。無ければ git clone --depth 1 -b 26.2 https://github.com/FabricMC/fabric /home/claude/fabric-26.2）の level rendering の例を読む
- 試合外・ロビーでは毒ガスは描かない（ロビーでもガイドは出す）
合格基準:
- [ ] 毒ガスの壁と外側の塗り、射程ガイド（通常=白／R押下中=黄）が入っている。パーティクルのガイドは消えている
- [ ] 26.2 API が apidump か Fabric の 26.2 ソースに実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
