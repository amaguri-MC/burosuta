# 指示書 W4: mode（バトルロイヤルの進行・コマンド・毒ガス・箱・キューブ）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT.md「W4 mode」と「ゲーム概要」を実装し、/brawl start から勝利・ロビー帰還まで遊べるようにする。
担当範囲: src/main/java/com/amaguri/brawl/mode/**, src/main/resources/brawl.mode.mixins.json, agent/lang/W4.json
制約:
- 選んでいないプレイヤーはランダムなキャラ。ボットのキャラもランダム、名前は「ボット1」〜
- スタート地点の割り当てはランダム。参加者を ArenaLayout.spawns へ。カウントダウン中は移動を止める（位置を毎tick戻す）
- 試合中の参加者プレイヤーは: アドベンチャー、持ち物は Brawlers.giveKit、アイテムを捨てる・拾うを禁止、ブロックの設置/破壊は不可
- 毒ガス: CONTRACT の時間どおり。縁にパーティクル（受け手の近くだけ）。ガスの中は Combat.damage(e, null, 1000, false) を20tickごと
- 脱落: MatchHooks.onEliminated を init で登録。順位=脱落時の生存数。キューブを落とす。倒した人に「〇〇を倒した！」、全員に小さく通知。プレイヤーはその場でスペクテイター
- 勝利: タイトル「#1 勝利！」、全員に順位表をチャットで。10秒後にロビー（/brawl setlobby の位置、無ければワールドのスポーン）へ戻し元のゲームモード・持ち物を復元。Combat.clearAll。ボットは消す
- 試合中にログアウトしたプレイヤーは脱落扱い。/brawl stop で即終了・復元
- 箱: Boxes.damage は ArenaLayout.boxTiles から位置→箱を引く。HP 4000。被弾で木の音とパーティクル。壊れたら 2x2x2 を消しキューブ1個を中心に落とす
- キューブの ItemEntity は拾えない設定（pickup delay 無限 等）にし、毎tick 近くの参加者を探して Combat.addCube＋音
合格基準:
- [ ] CONTRACT の W4 署名が一字一句そのまま存在、/brawl のサブコマンドが全部ある
- [ ] 開始→脱落→勝利→ロビー帰還の各段階で、二重実行や取り残し（ボットが残る、状態が残る）が無いことをコードで確認
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
