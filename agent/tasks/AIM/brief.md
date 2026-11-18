# 指示書 AIM: 本家風の照準ガイド（扇・直線・投てき・範囲）と照準距離
先に agent/tasks/COMMON.md と CONTRACT.md を読む。
背景: 今の射程ガイド（client/world/WorldOverlay）は全キャラ同じ「まっすぐの帯」。本家のように攻撃の形どおりのガイドにしたい。また AttackPayload / SuperPayload に `dist`（照準点までの水平距離、ブロック。0=不明）を司令塔が追加した（api/Net.java）。今は送る側・受ける側がコンパイルエラーの状態。
担当範囲: src/main/java/com/amaguri/brawl/brawler/**（新規 AimShapes と、Attacks/Brawlers の dist 対応）, src/client/java/com/amaguri/brawl/client/world/WorldOverlay.java, src/client/java/com/amaguri/brawl/client/view/TopDownClient.java と client/hud/HudClient.java の payload 送信部分だけ
やること:
1. `brawler/AimShapes`（common 側。クライアントからも読む）: キャラ id ごとに 通常/必殺 の形を返す。形の種類: FAN（扇: 角度・射程）, LINE（直線: 幅・射程。連射や衝撃波）, LOB（投てき: 最大射程と着弾の円の半径。照準点の位置に円、プレイヤーから照準点へ放物線の点線）, DASH（突進・転がり: 幅・距離）, JUMP（跳躍: 最大距離と着地の円）, CIRCLE_SELF（自分中心の円）, SUMMON（呼び出し: 照準点の円）。数値は Attacks / BrawlerStats の実際の処理から取る（ガイドと実際の当たりが一致すること）
2. WorldOverlay: 形どおりに地面へ半透明で描く（通常=白、R を押している間=必殺の形で黄色）。扇は細かい三角形の扇、LOB/JUMP/SUMMON は照準点を最大射程で切って円を描く。今の帯の描き方（submitCustomGeometry 等）を流用
3. dist の対応: 見下ろし中は TopDown.aimPoint() までの水平距離を、一人称のときは 0 を送る（TopDownClient と HudClient の送信）。サーバー（Brawlers の受信→attack/useSuper）は dist>0 なら照準点として使い（最大射程で切る）、0 なら今の「正面の最寄りの敵」方式。対象: バーリーの投げ・必殺、エドガーの跳躍、ルーの氷の床、ニタのクマの召喚位置、ブロックの必殺の中心。Brawlers.attack/useSuper の公開の署名を変えるなら、呼び出し元（bot/BrawlBot、gametest）も合わせて直す（距離0の版を残すのが簡単）
合格基準:
- [ ] 11キャラ × 通常/必殺 の形が AimShapes にあり、Attacks の当たりの形と一致（扇の角度・射程・幅を返答で1行ずつ）
- [ ] 全体がコンパイルできる見込み（Net の新しい署名で送受信すべて直っている。grep で `new Net.AttackPayload(` と `new Net.SuperPayload(` が2引数）
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内（＋形の一覧は stats 風に agent/preview/AIM/shapes.md に）
