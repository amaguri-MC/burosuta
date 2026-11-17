# 指示書 W7: 自動テスト（サーバー gametest＋クライアント撮影）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT の API だけを使って、動作を CI で確かめるテストを書く（実装は他担当が並行中。中身は見ずに CONTRACT の署名で書く）。
担当範囲: src/gametest/**（BrawlGameTests, BrawlClientTest）, .github/workflows/client-gametest.yml
手本: /home/claude/ref-aibot/src/gametest/** と .github/workflows/melt-shots.yml、build.gradle の gametest 設定（このリポにもある）
サーバー gametest（各テストはガラスで囲んだ小さな舞台。隣と混ざらないよう半径を小さく。26.2 の GameTestHelper の座標は相対）:
1. シェリーの参加者が正面6ブロックのボット(BrawlBot.spawn, colt)に Brawlers.attack → ボットの hp が 300 以上減る
2. 弾3回撃つと4回目は false、1.5秒後に撃てる
3. ゲージ1.0にして useSuper → 前方の WALL タイルが消える
4. Boxes.damage を4000 → 箱ブロックが消え、POWER_CUBE が出て、近くの参加者の cubes=1・maxHp+400
5. 茂みにいる参加者は8ブロック先の敵から isHiddenFrom=true、3ブロックなら false
6. ArenaMap.layout(): spawns=10、boxTiles 14〜18
7. Combat.damage で hp<=0 → MatchHooks.onEliminated が1回だけ（差し替えて数える）
クライアント gametest（BrawlClientTest）: 言語 ja_jp、hideGui は切る。シングルで /brawl start（ボット9）→ カウントダウン後 3秒・10秒で HUD 入りスクショ、マップ全体を真上からのスペクテイター視点で1枚。撮影後 /brawl stop
client-gametest.yml: workflow_dispatch で起動、xvfb-run で runClientGameTest、スクショを gametest-out ブランチに force push（melt-shots.yml を真似る）
合格基準:
- [ ] 7つのサーバーテストと撮影が CONTRACT の署名だけを使っている
- [ ] 26.2 の gametest API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
