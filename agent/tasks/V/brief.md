# 指示書 V: 本家風の見下ろし視点
先に agent/tasks/COMMON.md と CONTRACT.md（末尾の「第2弾」も）を読む。
目的: ブロスタのような斜め上の固定カメラで遊べるようにする（MC 26.2 Fabric クライアント）。
担当範囲: src/client/java/com/amaguri/brawl/client/view/**, src/client/resources/brawl.view.mixins.json（新規。fabric.mod.json と BrawlClient への登録は返答に書けば司令塔がやる）, agent/lang/V.json
仕様:
- 有効になる条件: 見下ろしON（既定ON、キー V で切替。キー設定に「見下ろし視点」）かつ（試合中で生存 = HudPayload.brawler が空でなく alive、またはロビー中 = LobbyPayload.inLobby）かつ 画面（Screen）が開いていない。状態は `com.amaguri.brawl.client.hud.HudState` の public static フィールド `hud`（Net.HudPayload, 試合外 null）と `lobby`（Net.LobbyPayload, U が追加。無ければ null）を読むだけにする（payload の受信登録は U がする。V は登録しない）
- カメラ: プレイヤーの南側の斜め上（向きは固定で北を向く = yaw 180、俯角 58°、距離 15 ブロック）から見下ろす。追従はなめらか（lerp）。Camera の setup への Mixin で位置・回転を差し替え、detached 扱いにして自分のキャラモデルが描かれるように。壁でカメラが寄らない
- カーソル: 有効中はマウスの掴みを外して矢印カーソルを出す（無効に戻ったら掴み直す）。マウス移動で視点は回らない
- 照準: カーソル位置から画面→ワールドの光線を作り（カメラ位置・向き・FOV・画面比から計算）、高さ プレイヤー足元+1.0 の水平面との交点を aimPoint、プレイヤーからの方向を aimYaw とする。毎フレーム プレイヤーの yRot/yHeadRot/体の向き を aimYaw に、xRot=0
- 移動: W=画面の上（北）、S=南、A=西、D=東。プレイヤーの向き（aimYaw）が何であっても画面基準で動くよう、入力の移動ベクトルを回して与える（LocalPlayer の入力適用部分を apidump で探して Mixin）。ジャンプはそのまま
- 攻撃: 有効中の左クリック/右クリック → AttackPayload(aimYaw)（押しっぱなしは4tickごと）。バニラの攻撃・使用・破壊は起こさない
- 照準ガイド: 地面に、プレイヤーから aimYaw 方向へ射程ぶん（Brawlers.get(id).attackRangeTiles() × Tiles.BLOCKS_PER_TILE。common のクラスなのでクライアントから読める）の白い半透明の帯（幅1ブロック）を描く。ワールド描画が難しければクライアント側のパーティクル（数tickごと）で点線にしてよい
- 26.2 の Camera / MouseHandler / LocalPlayer の入力 / ワールド描画 は apidump（client.txt）で実在確認。手本は /home/claude/ref-aibot/src/client
合格基準:
- [ ] 上の仕様がすべて入っている。無効時はバニラと完全に同じ挙動に戻る
- [ ] 画面→ワールドの光線の計算を、Python で同じ式を書いて「画面中央のカーソル → カメラ注視点付近の地面」になることを数値で確認（agent/preview/V/ray_check.py）
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内（登録が必要な mixin 設定名と init の呼び出しを含める）
