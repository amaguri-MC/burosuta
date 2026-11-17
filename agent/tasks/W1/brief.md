# 指示書 W1: core（HP・弾・ゲージ・弾道・茂み・同期）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT.md「W1 core」の全APIを実装する。試合中の参加者（プレイヤーもボットも）を同じ仕組みで扱う土台。
担当範囲: src/main/java/com/amaguri/brawl/core/**, src/main/resources/brawl.core.mixins.json, agent/lang/W1.json
入力: CONTRACT.md, src/main/java/com/amaguri/brawl/api/*.java
成果物: core パッケージ一式
制約:
- 状態は UUID→BrawlState の Map（サーバー側）。ボットの BrawlState も同じ
- 弾の回復: maxAmmo / reloadSec は Brawlers.get(brawlerId) から
- 被弾演出: 赤く光る（ダメージ時の hurt アニメ。26.2 の該当パケット/メソッドを apidump で探す）＋被弾音。数字のダメージ表示は不要
- 茂みルール: target が茂みの中 かつ 4ブロックより遠い かつ revealUntilTick を過ぎている → 見えない
- HiddenPayload は受け手ごとに作る（自分自身は入れない）。BarsPayload は受け手の48ブロック以内の alive 参加者で、隠れている者は除く
- 試合外の人には何も送らない（試合終了時に HudPayload(brawler="") を1回送って HUD を消す）
- 参加者のバニラダメージ無効・空腹固定（食料6で走れない）・走り不可
- Projectiles は ServerTickEvents で一括で進める。1tick で speed ぶん進むが、当たり判定は0.25ブロック刻みで抜けないように
合格基準:
- [ ] CONTRACT の W1 の署名がすべて一字一句そのまま public で存在
- [ ] damage→hp<=0 で MatchHooks.onEliminated が1回だけ呼ばれる（二重呼び出しなし）
- [ ] 弾が壁(Arena.isBulletSolid)で止まり、箱なら Boxes.damage、breaksWalls なら Arena.breakTile して進む
- [ ] 使った 26.2 API がすべて apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内
