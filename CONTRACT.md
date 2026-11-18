# CONTRACT（担当間の約束。変えるときは司令塔に相談）

modid `brawl` / base package `com.amaguri.brawl` / MC 26.2 Fabric（loader 0.20.1, fabric-api 0.161.0+26.2）。
API の裏取りは `/home/claude/apidump/mc/common.txt` `client.txt`（javap出力）と `/home/claude/apidump/fabric/fabric.txt`。**grep して実在を確認してから使う**。手本: `/home/claude/ref-aibot`（同じ 26.2 の動くMod。PathfinderMob＋自作Renderer、gametest、コマンド登録など）。

## ゲーム概要（ブロスタのソロ「バトルロイヤル（Showdown）」）
- 10人（足りない分はボット）がマップの端からスタート。全員が敵。最後の1人が勝ち。順位 #1〜#10
- パワーキューブ箱を壊すとキューブが出る。拾うと最大HP+400（同時に400回復）・与ダメ+10%（1個ごと）
- 脱落した人は持っていたキューブを全部その場に落とす
- 毒ガス: 開始40秒後から外周が縮み始め、120秒かけて中央の 8×8タイルまで縮む。ガスの中は1秒ごとに1000ダメージ
- 茂み: 中にいると敵から見えない（距離4ブロック以内の敵からは見える。攻撃すると1秒見える）
- 回復: 3秒間 攻撃も被弾もしなければ、毎秒 最大HPの13% 回復
- 弾は3発（ammo 0〜3 の小数）。1発ずつ reloadSec で回復
- 必殺技ゲージ: 通常攻撃が当たると溜まる。満タン(1.0)で R キー

## 単位
- **1タイル = 2ブロック**（`Tiles.BLOCKS_PER_TILE`）。射程などはタイルで決めてブロックに直す
- HP・ダメージはブロスタと同じ整数（3600 など）。**バニラの体力は使わない**（試合中の参加者はバニラのダメージ無効）
- 弾は水平に飛ぶ（yaw だけ使い、pitch は無視）。高さは撃った人の足元+1.0

## 担当と範囲（他担当のファイルに触らない）
| 担当 | package / ファイル |
|---|---|
| 司令塔 | `BrawlMod`, `client/BrawlClient`, `api/**`, `fabric.mod.json`, `build.gradle`, `.github/**`, CONTRACT.md |
| W1 core | `core/**`, `brawl.core.mixins.json` |
| W2 brawler | `brawler/**`, `assets/brawl/{items,models/item,textures/item}/**` |
| W3 arena | `arena/**`, `assets/brawl/{blockstates,models/block,textures/block}/**`, `data/brawl/**`（マップ） |
| W4 mode | `mode/**`, `brawl.mode.mixins.json` |
| W5 bot | `bot/**`, `client/bot/**`, `brawl.bot.mixins.json`, `brawl.botclient.mixins.json` |
| W6 hud | `client/hud/**`, `brawl.hud.mixins.json`, `assets/brawl/textures/gui/**` |
| W7 test | `src/gametest/**`, `.github/workflows/client-gametest.yml` |
- 言語ファイルは各担当が `agent/lang/<担当>.json`（キー→日本語）に書く。司令塔が ja_jp/en_us（両方日本語）にマージする

## api（司令塔が書いた。実物を読むこと）
- `api/Net.java`: HudPayload / MatchPayload / HiddenPayload / BarsPayload（S2C）、AttackPayload / SuperPayload（C2S）
- `api/Tiles.java`: `BLOCKS_PER_TILE = 2.0`, `static double b(double tiles)`（タイル→ブロック）

## W1 core `com.amaguri.brawl.core`
```java
public final class BrawlState {                 // 試合参加者1人ぶん（プレイヤーもボットも）
  public final java.util.UUID id; public final String brawlerId;
  public int hp; public float ammo; public float superCharge; public int cubes;
  public boolean alive; public long lastActTick; public long lastHurtTick; public long revealUntilTick;
  public int maxHp();                           // def.maxHp + 400*cubes
  public float damageMul();                     // 1 + 0.1*cubes
}
public final class Combat {
  public static void init();
  public static BrawlState join(LivingEntity e, String brawlerId); // 状態を作りHP満タン・弾3・ゲージ0。移動速度を適用
  public static void leave(LivingEntity e);      // 状態を消し、移動速度などを元に戻す
  public static BrawlState state(Entity e);      // 試合外は null
  public static java.util.List<LivingEntity> participants(ServerLevel lvl); // alive のみ
  public static void clearAll(MinecraftServer s);
  public static void damage(LivingEntity victim, LivingEntity attacker /*null可*/, int baseDamage, boolean fromMainAttack);
      // 実ダメ = round(baseDamage * attacker.damageMul)。被弾演出（赤く光る・音）。fromMainAttack なら attacker のゲージに def.superPerHit を足す（1.0で頭打ち）
      // hp<=0 で alive=false → MatchHooks.onEliminated(victim, attacker) を呼ぶ（W4 が登録）
  public static void heal(LivingEntity e, int amount);
  public static void addCube(LivingEntity e);    // cubes+1, hp+400
  public static void knockback(LivingEntity e, Vec3 dirUnit, double blocks); // 水平に押す
  public static void areaDamage(ServerLevel lvl, LivingEntity attacker, Vec3 center, double radiusBlocks, int baseDamage, boolean fromMainAttack); // 範囲内の敵（attacker以外）と箱
  public static boolean consumeAmmo(LivingEntity e);   // 1以上なら1減らして true。lastActTick 更新・1秒 reveal
  public static boolean consumeSuper(LivingEntity e);  // 1.0 なら 0 にして true
  public static boolean isHiddenFrom(LivingEntity target, LivingEntity viewer); // 茂みルール
  public static boolean inBush(LivingEntity e);        // 足元か頭の位置が ArenaBlocks.BUSH
}
public final class Projectiles {
  public static void spawn(ServerLevel lvl, Shot shot); // 毎tick進め、パーティクルを出し、当たり判定
}
public record Shot(LivingEntity owner, Vec3 origin, Vec3 dir /*水平の単位ベクトル*/, double speed /*ブロック/tick*/,
    double range /*ブロック*/, double radius /*当たり判定の太さ ブロック*/, int damage, boolean fromMainAttack,
    boolean breaksWalls, boolean pierce, net.minecraft.core.particles.ParticleOptions trail,
    OnHit onHit /*null可*/, OnEnd onEnd /*null可*/) {
  public interface OnHit { void hit(ServerLevel lvl, Shot s, LivingEntity target, Vec3 pos); } // 指定時は damage を自動では与えない
  public interface OnEnd { void end(ServerLevel lvl, Shot s, Vec3 pos, boolean hitWall); }   // 射程切れ・壁・命中（非貫通）で1回
}
public final class MatchHooks {                 // W4 が init で登録する差し込み口
  public static java.util.function.BiConsumer<LivingEntity, LivingEntity> onEliminated = (v, k) -> {};
}
```
- 弾の当たり判定: 進む線分ごとに、参加者（owner以外・alive）の当たり箱を radius だけ広げて交差。ブロック: `Arena.isBulletSolid(state)` で止まる。止まったブロックが箱なら `Boxes.damage(lvl, pos, dmg, owner)`（W4）、`breaksWalls` なら `Arena.breakTile(lvl, pos)`（W3）してそのまま進む
- 試合中の参加者はバニラのダメージ・空腹・落下ダメージ無効（ALLOW_DAMAGE で false）。走り（ダッシュ）不可（毎tick setSprinting(false)）
- 移動速度: 目標 ブロック/秒 = def.speedTiles × 2。プレイヤー基本 0.1 が 4.317 ブロック/秒。ボットは W5 の `BrawlBot.speedFor(blocksPerSec)` で
- 毎tick: 弾の回復、回復(regen)、毒ガスは W4、同期送信（HudPayload 2tick / BarsPayload 2tick / HiddenPayload 4tick）
- 頭上HPは BarsPayload でクライアント(W6)が描く。core は名前タグ・below_name を使わない

## W2 brawler `com.amaguri.brawl.brawler`
```java
public record BrawlerDef(String id, String nameJa, int maxHp, double speedTiles, int maxAmmo, double reloadSec,
    int attackCooldownTicks, double attackRangeTiles, double superRangeTiles, float superPerHit, String roleJa) {}
public final class Brawlers {
  public static void init();                     // アイテム登録・C2S受信（AttackPayload→attack, SuperPayload→useSuper）
  public static BrawlerDef get(String id);       // 無ければ null
  public static java.util.List<BrawlerDef> all();
  public static boolean attack(LivingEntity user, float yawDeg);   // 弾/クールダウン不足なら false
  public static boolean useSuper(LivingEntity user, float yawDeg); // ゲージ不足なら false
  public static void giveKit(ServerPlayer p);    // 持ち物を空にして スロット1=武器（見た目だけ）
  public static Item weaponItem(String brawlerId); // ボットの手にも持たせる
}
```
- 初期キャラ（id / 名前）: `rusty` ラスティ, `kaden` ケイデン, `gouen` ブル, `mimosa` ブロック, `takomaru` タコまる
- 武器アイテムは見た目だけ（使用・攻撃・設置などの効果を持たない。投げ捨て不可は W4）
- 弾数・ゲージ・HP の数値はこのファイルだけに置く（他担当は get() を通す）

## W3 arena `com.amaguri.brawl.arena`
```java
public final class ArenaBlocks { public static void init();
  public static Block WALL, BORDER, BUSH, BOX, ROPE, FLOOR_A, FLOOR_B;
  public static Item POWER_CUBE; }   // キューブの見た目アイテム（テクスチャも W3）
public final class Arena {
  public static boolean isBulletSolid(BlockState s);  // WALL, BORDER, BOX と バニラの完全ブロック
  public static boolean isBush(BlockState s);
  public static boolean breakTile(ServerLevel lvl, BlockPos pos); // WALL のタイル(2x2x高さ)を壊す。BORDER・BOX は false
}
public record ArenaLayout(BlockPos origin /*タイル(0,0)の北西下の角。床の上の高さ*/, int widthTiles, int depthTiles,
    java.util.List<net.minecraft.world.phys.Vec3> spawns /*10個*/, java.util.List<BlockPos> boxTiles /*箱タイルの北西下角*/,
    net.minecraft.world.phys.Vec3 center) {}
public final class ArenaMap {
  public static ArenaLayout layout();            // data/brawl/maps/showdown.txt を読む（建てない）
  public static ArenaLayout build(ServerLevel lvl); // 建てる（毎試合 作り直し）。既定の場所 x=10000 y=100 z=10000
}
```
- マップは 文字のタイル図（50×50タイル=100×100ブロック）。`.`床 `#`壁(壊せる) `=`外壁(壊せない) `B`茂み `X`パワーキューブ箱 `R`ロープ柵（通れないが弾は通る） `S`スタート地点
- 壁・茂み・箱・柵 の高さは2ブロック。外壁は3ブロック＋上にバリア
- 箱は 2×2×2 ブロックの BOX ブロック

## W4 mode `com.amaguri.brawl.mode`
```java
public final class Showdown { public static void init(); public static boolean running(); }
public final class Boxes { public static void damage(ServerLevel lvl, BlockPos pos, int dmg, LivingEntity by); } // 箱HP 4000。壊れたら8ブロック消してキューブを落とす
public final class Gas { public static boolean inGas(Vec3 pos); public static net.minecraft.world.phys.AABB safeZone(); }
```
- コマンド（`/brawl`）: `/brawl`（キャラ一覧をクリックで選べるチャット）, `/brawl pick <id>`, `/brawl start [ボット数 既定=10人に足りない分]`（OP）, `/brawl stop`（OP）, `/brawl setlobby`（OP）, `/brawl bots <easy|normal|hard>`（OP）, `/brawl map`（OP: マップだけ建てる）
- 流れ: start → ArenaMap.build → 参加者（オンラインの全プレイヤー＋ボット）をスタート地点へ → アドベンチャー・持ち物を武器だけ → 3秒カウントダウン（動けない）→ 試合 → 脱落したプレイヤーはスペクテイター、ボットは消す → 残り1人で勝利演出（タイトル「#1 勝利！」）→ 10秒後にロビーへ戻し元のゲームモード
- キューブ: 箱から出たキューブ（`ArenaBlocks` の POWER_CUBE アイテムの ItemEntity。拾えないようにして）に参加者が1.2ブロック以内に近づいたら `Combat.addCube`
- MatchPayload を10tickごとに送る。毒ガスの縁に緑のパーティクル

## W5 bot `com.amaguri.brawl.bot`, `com.amaguri.brawl.client.bot`
```java
public final class BotEntities { public static void init(); public static EntityType<BrawlBot> BRAWL_BOT; }
public class BrawlBot extends PathfinderMob {
  public static BrawlBot spawn(ServerLevel lvl, Vec3 pos, String brawlerId, String name);
  public static double speedFor(double blocksPerSec);  // MOVEMENT_SPEED の値
  public String getBrawlerId(); public String getBotName();
  public static String difficulty = "normal";           // easy|normal|hard（W4 のコマンドが書く）
}
public final class BotClient { public static void init(); } // client 側。プレイヤーモデル＋既定スキンで描く
```
- AI は `Brawlers.attack/useSuper` を呼ぶだけで戦う（ズルしない）。見えない敵（茂み）は狙わない

## W6 hud `com.amaguri.brawl.client.hud`
- `HudClient.init()`: 受信（Hud/Match/Hidden/Bars）、HUD描画、入力（試合中は左クリック/右クリック→AttackPayload、R→SuperPayload。バニラの攻撃・使用・ブロック破壊は止める）、隠れている実体を描かない、頭上HPバー

---
# 第2弾（見下ろし視点・本家風UI・ロビー）
## 担当と範囲
| 担当 | 範囲 |
|---|---|
| 司令塔 | api/Net.java（KillFeed/Result/Lobby/OpenSelect/Pick を追加済み）, core の MatchHooks.canHit・Combat.setDisplay/clearDisplay/isDisplay（追加済み） |
| V view | `client/view/**`, `brawl.view.mixins.json`（client） |
| U ui | `client/hud/**`（W6 のものを作り直してよい）, `client/select/**`, `assets/brawl/textures/gui/**` |
| L lobby | `mode/**`（W4 のものを変えてよい）, `lobby/**`, `brawl.lobby.mixins.json`, `data/brawl/lobby/**`, ロビー用ブロックは `lobby/` 内で登録（arena のブロックは使ってよい） |

## 約束
- `com.amaguri.brawl.client.view.TopDown`（V）: `public static boolean active()`（見下ろし中か）, `public static float aimYaw()`（照準の向き 度）, `public static net.minecraft.world.phys.Vec3 aimPoint()`（カーソルが指す地面の点、無ければ null）
  - 見下ろし中のクリック攻撃・照準は V が持つ（AttackPayload を送る）。U の入力処理は `TopDown.active()` のとき左/右クリックを扱わない（R の必殺は U のまま。yaw はプレイヤーの yRot = V が照準に合わせて毎フレーム設定する）
- ロビーにいるプレイヤーは「練習中の参加者」（L が Combat.join する）。HudPayload は来るが、LobbyPayload.inLobby=true のあいだ U はロビー用の画面を出す
- `Combat.setDisplay(e, id)` の実体はキャラのモデルで描かれる（像）。参加者ではない
- `MatchHooks.canHit(attacker, victim)` が false ならダメージは通らない（練習場のルールに L が使う）
- ステータス（トロフィー・勝利数・選択キャラ）は L がサーバーの config/brawl_stats.json に保存
- `client/hud/HudState`（U）: payload の受信は U だけが登録し、`public static Net.HudPayload hud`（試合外 null）, `public static Net.MatchPayload match`, `public static Net.LobbyPayload lobby`（ロビー外 null）を持つ。V はこれを読むだけ

---
# 第3弾（モブの見た目・ニタ/バーリー追加・専用描画のガイド）
- キャラの見た目は「バニラのモブ」。id→モブ: rusty→PILLAGER, kaden→SKELETON, gouen→HOGLIN, mimosa→BLAZE, takomaru→IRON_GOLEM, nita→POLAR_BEAR, barley→WITCH。ニタの相棒 `nitabear` は自作モデル（assets/brawl/models/brawler/nitabear.json）
- 名前は「モブ名（役割）」: 襲撃者（ショットガン）/ スケルトン（ガンマン）/ ホグリン（突進タンク）/ ブレイズ（ロケット）/ アイアンゴーレム（格闘）/ シロクマ（クマ使い）/ ウィッチ（投てき）。内部 id はそのまま
- 全員 見た目の高さを約1.9ブロックに揃える（当たり判定はプレイヤーと同じ）
- BarsPayload.team: 0=自分 1=敵 2=味方（自分の召喚物）
| 担当 | 範囲 |
|---|---|
| MOB | client/model/**（描画の振り分け・モブ人形）, client/hud/Faces.java, client/select/**（3D表示） |
| NB | brawler/**, core/**（召喚の味方判定）, bot/BrawlBot.java（相棒クマの思考）, src/client/resources/assets/brawl/models/brawler/nitabear.json と textures/entity/brawler/nitabear.png, 言語 |
| G | client/view/TopDownClient.java の guide 部分, client/world/**（新規） |
