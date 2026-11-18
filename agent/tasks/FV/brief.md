# 指示書 FV: モブの見た目の不具合（ウィザースケルトン・ウィッチ・ブレイズ）とキャラ選択のスクロール
先に agent/tasks/COMMON.md と CONTRACT.md を読む。
作業場所: `git -C /home/claude/burosuta worktree add /home/claude/wt-fv -b fix-visual` で作った /home/claude/wt-fv で作業し、fix-visual ブランチに push する（main には push しない。最後に司令塔がマージ）。commit は必ず `git add -A`。
実機確認: `gh workflow run client-gametest.yml -R amaguri-MC/burosuta --ref fix-visual` → 終わったら `git clone -q --depth 1 -b gametest-out https://github.com/amaguri-MC/burosuta <新しいディレクトリ>` でスクショを取り、Read で見る。撮影内容は src/gametest/java/com/amaguri/brawl/gametest/BrawlClientTest.java（このブランチで自由に足してよい。例: 問題の3体を一人称で近くから正面・横の2枚）
1. 見た目: ユーザーから「ウィザースケルトン（edgar）、ウィッチ（barley）、ブレイズ（mimosa）の見た目が変」と報告。現状のスクショ /home/claude/lfp.png（一人称の並び）では、ブレイズは棒だけ見えて頭が離れて浮いている／ウィザースケルトンは小さく変な姿勢、に見える。client/model/MobPuppets.java の人形の作り方（状態の写し、倍率、各モブ専用の描画状態の値＝ブレイズの回転・ウィッチの鼻やアイテム・スケルトンの構え/aggressive など）を見直し、バニラでその場に置いた時と同じ自然な見た目にする。必要ならスクショに比較用のバニラのモブ（summon）を横に並べる
2. キャラ選択画面（client/select/SelectScreen.java）: 11人の顔の一覧が画面に入りきらずスクロールできない。マウスホイールでスクロールできる一覧か、入りきる格子（2〜3列）にする。小さい画面（854x480）でも全員選べること。スクショで確認
合格基準:
- [ ] 3体がバニラのモブと同じ自然な見た目（スクショを Read して確認、返答に何を直したか1行）
- [ ] 選択画面で11人全員に届く（スクショで確認）
- [ ] fix-visual ブランチでビルドとサーバー gametest が通っている
返答: 「完了 / 成果物（ブランチ名・最後のコミット）/ 不確かな点」を3行以内
