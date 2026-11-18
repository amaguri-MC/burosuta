# 検品 第2弾（V/U/L のつなぎ目）

判定: FAIL

## 1. 像の右クリックでキャラ選択が開かない（重要）
- ファイル:行: client/hud/mixin/MinecraftMixin.java:366-369（`startUseItem` を `HudState.blockVanillaInput()` で止める）、client/hud/HudState.java:203-205（`blockVanillaInput = alive()`）、client/view/mixin/ViewMinecraftMixin.java:339-342、client/view/TopDownClient.java:176（keyUse を食べて AttackPayload にする）
- 問題: ロビーのプレイヤーは練習中の参加者なので HudPayload.alive=true が来る → 見下ろしON/OFFどちらでも `startUseItem` が止まり、エンティティへの interact パケットが出ない。サーバー側の `Lobby` の UseEntityCallback（Lobby.java:98-105）は一度も呼ばれず、OpenSelectPayload が来ない。見下ろし中は右クリックが攻撃になり、hitResult もプレイヤーの向き基準でカーソル位置と関係ない。
- 直し方: ロビー中（`HudState.inLobby()`）の右クリックは攻撃にしない。V がカーソルの光線（TopDown.rayDir、カメラ位置から）で `ProjectileUtil.getEntityHitResult` 等で実体を拾い、当たったら `mc.gameMode.interact(player, entity, hand)`（または専用の C2S）を送る。見下ろしOFF時は U の MinecraftMixin で「inLobby かつ hitResult が ENTITY」のとき startUseItem を通す。

## 2. 画面中央のタイトルが二重（サーバーのバニラ title と U の大きな表示が重なる）
- ファイル:行: mode/Showdown.java:333（3・2・1）, 345（スタート！）, 469-471（#n 脱落）, 558-563（#1 勝利！/#n）; lobby/Lobby.java:362-363（ロビーの5・4…）; client/hud/HudClient.java:267-270（TITLE を置き換えていない）
- 問題: U が MatchHud/LobbyHud/ResultScreen で同じ情報を中央に大きく描くのに、サーバーもバニラの title/subtitle を送るので、文字が重なって読めない（「3」と「3」、「スタート！」と「ファイト！」、「#5」と「#5 敗退」）。
- 直し方: L 側で title 送信をやめる（音だけ残す）か、U 側で HudState.active()/inLobby() 中は `VanillaHudElements` の TITLE/SUBTITLE 相当を置き換えて隠す。どちらか片方に統一。

## 3. 結果画面が最初のクリックで即閉じる
- ファイル:行: client/hud/ResultScreen.java:34-38
- 問題: ResultPayload は最後の1人が倒れた瞬間に来る。勝者は撃ち続けている最中なので、次の連打クリックで 0.1 秒程度で閉じてしまい、順位・トロフィーが見えない。
- 直し方: `age < 20`（1秒）のあいだは mouseClicked を無視する（Esc は可）。

## 4. ロビーからサーバーを抜けても練習の Combat 状態が残る
- ファイル:行: lobby/Lobby.java:96
- 問題: DISCONNECT で IN_LOBBY から外すだけで Combat.leave しない（Combat 側は試合用に状態を残す設計）。入り直すと upkeep は「状態あり・同じキャラ」で join し直さないので、新しい実体に速度・空腹の設定が入らないまま練習扱いになる。
- 直し方: `!Showdown.running()` なら DISCONNECT で `Combat.leave(p)` する。

## 5. 試合→ロビー帰還の瞬間に見下ろしが一瞬切れてマウスが掴まれ直す
- ファイル:行: mode/Showdown.java:606 → lobby/Lobby.java:338 付近（入室処理は次の tick、LobbyPayload は 10tick ごと）
- 問題: clearAll で HudPayload("") → hud=null。LobbyPayload(inLobby=true) が届くまで最大 10tick、TopDown.active()=false になり一人称に戻り、TopDownClient.java:160 で grabMouse → すぐ release。カメラがガクッと飛ぶ。
- 直し方: Lobby の「入った人」処理で即 sendLobby(true, …) を1回送る。

## 6. 観戦者（試合中に入ってきた人）が B でキャラ選択を開ける
- ファイル:行: client/hud/HudClient.java:283
- 問題: 観戦者は hud=null なので inMatch()=false → 開ける。選んでもサーバーは Showdown.running() で黙って無視（Lobby.java:182）し、何も起きない。
- 直し方: `HudState.phase() != 0` のときも開かない、またはサーバーが「試合中は選べません」と返す。

## 7. カメラ距離が仕様と違う
- ファイル:行: client/view/TopDown.java:16（11.5。仕様は 15）
- 問題: 仕様差。意図的なら CONTRACT/指示書に反映する。
- 直し方: 15 にするか仕様を更新。

## 確認して問題なしだった点
- U と V の AttackPayload 二重送信: 無し（U は TopDown.active() のときクリックを扱わず、条件は同じ tick で同じ）。
- R の必殺の向き: V が毎フレーム yRot=aimYaw を入れ、U は getYRot() を送るので一致。
- WASD の画面基準の回転式（rotateInput）: 符号は正しい（W=北 -z, A=西 -x）。
- 脱落→スペクテイター: HudPayload.alive=false で active()=false → grabMouse で掴み直す。画面を閉じたときの grabMouse は active 中キャンセルされ、カーソルは出たまま。
- 練習場の canHit: プレイヤー→カカシのみ通る。像は Combat 状態が無いので当たらない。試合中は全部通る。
- 試合開始/終了: clearAll→prepare(join+kit+ADVENTURE)、終了は returnToLobby(ADVENTURE+MatchPayload phase0)→clearAll→Lobby.tick の upkeep で練習に戻る。inLobby=false は開始の次 tick に1回送られる。途中切断→再入室は観戦→終了後ロビー。
