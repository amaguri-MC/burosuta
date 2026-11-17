# 検品レポート（担当間のずれ）

判定: **FAIL**。そのままではビルドが通らず、通っても、ボットが入った試合は誰も勝てずに終わらない。

重要度順。行番号は検品時点のもの。

## 1. [コンパイルエラー] `Msg.t` の戻り値が `Component` なのに、呼ぶ側で `.withStyle()` を付け、`MutableComponent` に代入している
- ファイル:行: `mode/Msg.java:13`。呼んでいる所: `mode/Showdown.java:222, 250, 327, 427, 430, 434, 436, 444, 496, 507, 510-511`, `mode/ShowdownCommands.java:72, 80`
- 問題: `withStyle` は `MutableComponent` にしかない（apidump の `Component` インターフェースには無い）。`MutableComponent info = Msg.t(...)` も型が合わない。mode パッケージ全体がコンパイルできない。
- 直し方: `static MutableComponent t(...)` に変える（`Component.translatableWithFallback` は元々 `MutableComponent` を返す）。

## 2. [遊べない] ボットの Entrant ID がボット実体の UUID と違うので、ボットが脱落扱いにならず試合が終わらない
- ファイル:行: `mode/Showdown.java:194`（`new Entrant(UUID.randomUUID(), ...)`）、`:208-209`、`:398`（`m.byId.get(victim.getUUID())`）、`:400`
- 問題: `onEliminated` は実体の UUID で `byId` を引くので、ボットだと null になって return する。Combat 側では `alive=false` になる。そのため:
  - ボットは止まって立ったまま残り、`participants` から外れるので、誰も当てられない（ガスも `Combat.damage` が alive=false で無視する）
  - `m.alive` からボットが外れず、`alive.size() <= 1` にならないので、**試合が永遠に終わらない**（勝利・ロビー帰還まで進まない）
  - ボットが倒した場合も killer が引けず、「〇〇を倒した！」が出ない
- 直し方: `BrawlBot.spawn` を先に呼び、`new Entrant(bot.getUUID(), ...)` で登録する。または spawn の後で `m.byId` をボットの UUID で付け直す（`m.alive` は start の最後に `e.id` から作られるので、その前に済ませる）。

## 3. [取り残し] 試合中にログアウトするとキューブが落ちない（Combat と Showdown の DISCONNECT の順番）
- ファイル:行: `core/Combat.java:64-70`、`mode/Showdown.java:466-468`
- 問題: Combat.init は Showdown.init より先に呼ばれるので、Combat の DISCONNECT が先に `STATES.remove` する。そのあと Showdown が `Combat.state(p)` を見ると null で、cubes=0 として脱落処理される。キューブは消えたまま落ちない。
- 直し方: Combat の DISCONNECT では状態を消さず、W4 の `Combat.leave` に任せる（試合外の後始末は onTick の掃除で足りる）。または W4 がイベントのフェーズ（`Event.addPhaseOrdering`）で先に動くようにする。

## 4. [ずれ] `giveKit` が `join` より先に呼ばれていて、武器が渡らない
- ファイル:行: `mode/Showdown.java:245-246`（`Brawlers.giveKit(p)` の次に `Combat.join(p, ...)`）。`brawler/Brawlers.java:239-243` は状態が無ければ持ち物を空にして return する
- 問題: 最初の giveKit で持ち物が空になるだけで、武器は入らない。次の tick に `tickWorld` の hasWeapon が拾い直すので実害は1tickだけだが、契約の順番がずれている。
- 直し方: `Combat.join` → `Brawlers.giveKit` の順に入れ替える。

## 5. [ずれ] ボットが2回 join され、spawn 失敗時は NPE で試合開始ごと失敗する
- ファイル:行: `mode/Showdown.java:208-210`、`bot/BrawlBot.java:124`
- 問題: `BrawlBot.spawn` の中で `Combat.join` 済みなのに、Showdown がもう一度 join する（状態の作り直しになる。今は害が無い）。`spawn` が null を返したときは `Combat.join(null, …)` が NPE になり、試合の開始全体が cleanup に落ちる。
- 直し方: Showdown 側の `Combat.join(bot, ...)` を消し、`bot == null` ならその枠を飛ばす（Entrant を作らない）。

## 6. [遊び] カウントダウン中もボットが撃てる
- ファイル:行: `bot/BrawlBot.java:174-190`、`brawler/Brawlers.java:212-236`
- 問題: プレイヤーは HUD（phase 1/3）で入力を止めているが、サーバー側は試合のフェーズを見ていない。ボットはスタート地点に固定されたまま、射程内の箱や敵を撃ってしまう。
- 直し方: W4 に `Showdown.playing()`（PLAYING のときだけ true）を足し、BrawlBot.think の先頭か Brawlers.attack/useSuper で確かめる。契約変更なので司令塔に相談。

## 7. [テスト不一致] gametest 4（箱→キューブ→拾う）は今の W4 の実装では必ず落ちる
- ファイル:行: `src/gametest/.../BrawlGameTests.java:235`、`:262-270`。`mode/Boxes.java:116-128`、`mode/Showdown.java:365`
- 問題: (a) `Boxes.damage` は ArenaLayout.boxTiles（x=10000 付近）にある箱しか認めない。テストの舞台の箱は find が null になり壊れない。(b) キューブの拾得（`Cubes.tick`）は試合中の `Showdown.tickWorld` からしか呼ばれないので、試合外のテストでは拾われない。
- 直し方: どちらかに合わせる。W4: レイアウトに無い BOX ブロックも、2x2 の角を位置から求めて扱う。拾得は `Combat.participants` を相手に、試合外でも動かす（または試合中だけと契約に明記する）。W7: テストを `/brawl start` 相当の試合の中で行う。

## 8. [テスト] `setNoAi(true)` にしてもボットの思考が動く
- ファイル:行: `bot/BrawlBot.java:174-190`、`BrawlGameTests.java:65-66`
- 問題: `tick()` の自前の思考は `isNoAi()` を見ていない。テスト1では標的のボットも撃ち返し、向きも勝手に変わる（移動は navigation が止まっているので起きない）。テストが不安定になる。
- 直し方: `think` の前に `if (isNoAi()) return;` を入れる。

## 9. [順位] 最後の2人が同じ tick に倒れると、後に処理した方が #1 になり、同時に「勝者なし」と出る
- ファイル:行: `mode/Showdown.java:409`、`:487-493`
- 問題: rank = 脱落時の alive.size() なので、2人目は 1 になる。`m.alive` は空なので winner=null になり、表示が矛盾する。
- 直し方: `alive.size()==1` のまま最後の1人が脱落した場合は、その人を winner(#1) として扱うか、rank を `max(2, …)` にして「勝者なし」に揃える。

## 10. [軽微] 弾が箱に当たったときのダメージにキューブ倍率が掛からない
- ファイル:行: `core/Projectiles.java:121`、`core/Combat.java:261`
- 問題: 参加者へのダメージは `damageMul` が掛かるが、箱へは baseDamage のまま。
- 直し方: `Math.round(dmg * Combat.state(owner).damageMul())` で渡す（owner の状態が無ければ ×1）。

## 11. [軽微] アリーナのチャンクを強制読み込みしたまま解除しない
- ファイル:行: `arena/ArenaMap.java:89-94`
- 問題: 試合が終わっても 7x7 チャンクが読み込まれたまま残る。
- 直し方: W4 の cleanup から呼べる `ArenaMap.release(lvl)` を足す（契約の追加）。または解除はしない方針を明記する。

## 問題なしと確かめた点
- **yaw の向き**: `Attacks.dir` = (-sin, cos)（MC と同じ、yaw 0 = +Z）。ボットの `atan2(-dx, dz)`、HUD の `player.getYRot()`、Showdown のスタート時の向き、gametest の `yaw 0 = 南` がすべてこれに合っている。
- **W4 の「捨てたアイテムを毎tick消す」**: `Cubes.tick` が見るのは `m.level` の中の `m.arena`（アリーナ ±4 ブロック）だけで、試合中（match != null）しか動かない。アリーナの外や試合外のアイテムは消さない。
- **HP0 の二重処理**: `Combat.damage` は `alive` を見てフックを1回だけ呼ぶ。Showdown の `eliminate` は `v.eliminated` で止めているので、onEliminated・checkMissing・onDisconnect が重なっても1回で済む。
- **ボットの削除**: applyElim→removeBot（leave＋discard）、cleanup でアリーナ内の BrawlBot を一掃、ボット自身も state=null で discard。三重になっているが、どれも何度呼ばれても安全。
- **終了処理**: cleanup で restore（ゲームモード・持ち物・ロビー）→ `Combat.clearAll`（速度を戻し、HUD を消す）の順。次の start でも clearAll と Boxes.reset を通る。
- **26.2 の API**: 主な呼び出しは apidump で実在を確認した（broadcastDamageEvent, snapTo, teleportTo(8引数), connection.teleport, setNeverPickUp/setUnlimitedLifetime, RespawnData, sendParticles(ServerPlayer,…), HudElementRegistry.replaceElement/HudElement.extractRenderState, KeyMapping.Category.register, EntityRenderDispatcher.shouldRender/getRenderer, Minecraft.startAttack/continueAttack/startUseItem, AllowDamage, PlayerBlockBreakEvents.Before ほか）。見つかった型の不一致は #1 だけ。
