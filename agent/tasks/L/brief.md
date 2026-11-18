# 指示書 L: ロビー（広場・像・練習場・準備床・トロフィー）と試合との接続
先に agent/tasks/COMMON.md と CONTRACT.md（末尾「第2弾」も）を読む。今の mode/**（W4作）を読んでから、必要なら作り変えてよい。
担当範囲: src/main/java/com/amaguri/brawl/mode/**, src/main/java/com/amaguri/brawl/lobby/**, src/main/resources/brawl.lobby.mixins.json（必要なら新規。登録は返答に書く）, src/main/resources/data/brawl/lobby/**, ロビー用の新ブロックのテクスチャ等（assets/brawl の blockstates/models/textures に lobby_ で始まる名前で）, agent/lang/L.json, agent/preview/L/**
ロビーの広場（LobbyMap.build(ServerLevel)、場所 x=10000 y=100 z=9840 付近、アリーナ(z=10000〜)とは離す。だいたい 44×44 ブロック）:
- 本家の明るく楽しい雰囲気（明るい床の模様、色付きの縁、旗・植木・ベンチ）。オリジナルのデザイン
- スポーン地点（中央手前）
- キャラの像 5体: 台座の上に BrawlBot.spawn → Combat.leave → Combat.setDisplay(bot, id) で「見た目だけ」の像にし、setNoAi・動かない・無敵・保存しない。像の上に名前の TextDisplay。右クリック（UseEntityCallback）→ そのプレイヤーへ OpenSelectPayload(id)
- 練習場: カカシ（BrawlBot をダミーとして3体、NoAi、キャラは gouen と kaden と takomaru、HP は自然回復で戻る）。ロビーのプレイヤーは「練習中の参加者」（Combat.join(p, 選択キャラ)）で、武器で撃てる。MatchHooks.canHit: ロビーでは「プレイヤー→カカシ」だけ通す（プレイヤー同士・カカシ→人は通さない）。試合中は今までどおり全部通す
- プレイ床（4×4、光るブロック＋「プレイ」の TextDisplay）: 乗っている間は準備完了。ロビーの全員（1人以上）が準備完了 → 5秒カウントダウン → Showdown 開始（ボットで10人まで補充）。誰かが降りたら中止
- 掲示板: トロフィー上位5人（TextDisplay）
- サーバー起動時・/brawl lobby（OP）でロビーを建てて像とカカシを置き直す（実体は保存しない）。プレイヤーがサーバーに入ったら、試合中でなければロビーへ（アドベンチャー、持ち物は武器だけ）
試合との接続:
- 参加者はロビーにいるプレイヤー全員（試合中に入ってきた人は観戦→終了後ロビー）
- 脱落ごとに KillFeedPayload(倒した人の表示名, キャラ, 倒された人, キャラ) を全員へ。毒ガス等は killer=""
- 終了時に ResultPayload を参加プレイヤーへ。トロフィー: 1位+10, 2位+8, 3位+7, 4位+6, 5位+4, 6位+2, 7位0, 8位-1, 9位-2, 10位-3（0未満にはしない）、1位は勝利数+1。config/brawl_stats.json に UUID→{name, trophies, wins, selected} を保存
- 終了表示のあと全員ロビーへ戻し、練習中の参加者に戻す
- PickPayload を受けて選択キャラを保存（試合中は無視）、練習のキャラも切り替え、チャットに「〇〇を選んだ！」
- LobbyPayload を10tickごとにロビーのプレイヤーへ（ロビーを出たら inLobby=false を1回）
- 既存コマンドは残す（/brawl start は手動開始）。/brawl setlobby は「このロビーのスポーン」を上書き
見本: agent/preview/L/lobby_top.png（真上図、1ブロック=8px、色分けと凡例）を Python で描き Read で確認
合格基準:
- [ ] 上の仕様がすべて入っている。像・カカシが二重に湧かない（再起動・/brawl lobby 連打）
- [ ] 試合開始→終了→ロビー帰還で、練習の参加状態・ゲームモード・持ち物が正しく戻る（コードで追って確認）
- [ ] canHit の分岐がロビーと試合で正しい
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
