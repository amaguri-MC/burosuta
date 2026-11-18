# 指示書 FB: パワー11のステータスとボットの移動速度
先に agent/tasks/COMMON.md と CONTRACT.md を読む。
作業場所: `git -C /home/claude/burosuta worktree add /home/claude/wt-fb -b fix-balance` で作った /home/claude/wt-fb で作業し、fix-balance ブランチに push（main には push しない。最後に司令塔がマージ）。commit は必ず `git add -A`。サーバー gametest は push で走る（gh run list -R amaguri-MC/burosuta --branch fix-balance で確認、失敗は gh run view <id> --log-failed）。client-gametest は使わない（別の作業者が使用中）
1. パワー11: 本家はパワー1から1上がるごとに 基礎値の+10%（パワー11 = 基礎の2倍）。brawler/BrawlerStats の HP・全ダメージ（通常・必殺・爆発・毒・突進・衝撃波・相棒クマの HP と攻撃）・回復量（ポコ・ライフスティールの固定回復）を2倍にする。倍率は1か所の定数 `POWER = 11` から計算（`1 + 0.1 * (POWER - 1)`）し、数値表 agent/tasks/R1/stats.md の値は「パワー1の基礎値」のまま残す。パワーキューブは「最大HP +10%（そのキャラのパワー11の最大HPの10%）・与ダメ +10%」に（core/BrawlState.maxHp）。箱の HP は 6000、毒ガスは 1秒 1000 のまま（mode）。頭上表示・HUD は数値をそのまま出すので変更不要
2. ボットの移動速度: 「ボットが遅すぎる」と報告。bot/BrawlBot.speedFor はプレイヤーの比（0.1 = 4.317 ブロック/秒）で計算しているが、Mob の実速度は違う。サーバー gametest に計測テストを足す: 平らな床に BrawlBot を置き navigation.moveTo で 30 ブロック先へ向かわせ、20tick ごとに位置をログ（learned: 長い走路は専用の高さに分けて隣のテストと干渉させない）。実測のブロック/秒が キャラの速さ（タイル/秒 × 2）と ±10% で一致するよう speedFor を補正し、テストは「一致」を assert にして残す。プレイヤーの速さ（core が属性に入れる値）も、同じ考えでプレイヤーが 4.317 ブロック/秒=0.1 を前提にしているか確認
合格基準:
- [ ] パワー11の倍率が1か所の定数で全キャラに効いている（返答に代表値: 襲撃者 HP と通常1発のダメージ）
- [ ] ボットの実測速度テストが通る（実測値を返答に）
- [ ] fix-balance ブランチでビルドと全 gametest が通っている
返答: 「完了 / 成果物（ブランチ名・最後のコミット）/ 不確かな点」を3行以内
