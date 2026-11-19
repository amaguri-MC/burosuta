package com.amaguri.brawl.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.api.Tiles;
import com.amaguri.brawl.arena.ArenaBlocks;
import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.arena.ArenaMap;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;
import com.amaguri.brawl.mode.Gas;

import static com.amaguri.brawl.brawler.BrawlerStats.BEAR_BITE_INTERVAL_TICKS;
import static com.amaguri.brawl.brawler.BrawlerStats.BEAR_DMG;
import static com.amaguri.brawl.brawler.BrawlerStats.BEAR_REACH_BLOCKS;
import static com.amaguri.brawl.brawler.BrawlerStats.BEAR_SIGHT_BLOCKS;

/**
 * 試合の人数合わせ用ボット。バニラのAIゴールは使わず、毎tickの自前の思考で動く。
 * 思考の優先順: 1 毒ガス回避 / 2 低HPで退避 / 3 見える敵と戦う / 4 キューブ / 5 箱 / 6 中央へ。
 * 攻撃は Brawlers.attack / useSuper を呼ぶだけ（ズルしない）。茂みに隠れた敵は Combat.isHiddenFrom で除外する。
 */
public class BrawlBot extends PathfinderMob {
	/** easy|normal|hard（W4 のコマンドが書く）。 */
	public static String difficulty = "normal";

	/** プレイヤーの基本速度 0.1 が 4.317 ブロック/秒（プレイヤーは属性値に比例して速くなる）。 */
	private static final double PLAYER_BASE_SPEED = 0.1;
	/**
	 * Mob の実速度（ブロック/秒）= MOB_BPS_PER_ATTR_SQ * 属性値^2。
	 * 26.2 の Mob は MoveControl が属性値×速度倍率を setSpeed に入れ、移動時にさらに属性値が掛かるため、属性値の2乗に比例する
	 * （サーバー gametest の実測: 属性 0.1112 で 0.545 ブロック/秒 → 44.05）。
	 */
	private static final double MOB_BPS_PER_ATTR_SQ = 44.05;
	private static final int REPATH_TICKS = 20; // 経路の再計算は1秒に1回まで
	private static final double LOW_HP_RATIO = 0.35;
	private static final double GAS_LOOKAHEAD_SEC = 5.0;

	/** true なら自前の思考をしない（gametest が navigation を直接動かすための口）。 */
	public boolean externalControl;

	private String brawlerId = "";
	private String botName = "";

	// 思考の状態（サーバー側のみ・保存しない）
	private int nextPathTick;
	private int lastTargetId = -1;
	private int reactUntilTick;
	private int nextAttackTick;
	private int strafeDir = 1;
	private int strafeFlipTick;
	private Vec3 wander;
	private AABB lastZone;
	private int lastZoneTick;
	private double zoneShrinkPerSec;
	private BlockPos bushGoal;
	private int bushScanTick = -1000;
	/** 詰まり検知: 前回の位置と時刻、無視する目標（キー→解除tick） */
	private Vec3 stuckPos = Vec3.ZERO;
	private int stuckCheckTick;
	private final Map<Long, Integer> ignored = new HashMap<>();
	private long currentGoalKey = Long.MIN_VALUE;
	private List<BlockPos> boxTiles;

	public BrawlBot(EntityType<? extends BrawlBot> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, PLAYER_BASE_SPEED)
				.add(Attributes.FOLLOW_RANGE, 100.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	// ---- 契約 API ----

	/** 目標の速さ（ブロック/秒）→ MOVEMENT_SPEED の値。移動は navigation の速度倍率 1.0 で行う。 */
	public static double speedFor(double blocksPerSec) {
		return Math.sqrt(Math.max(0.0, blocksPerSec) / MOB_BPS_PER_ATTR_SQ);
	}

	/** 速度が mul 倍（減速・加速・停止）になるように、属性値に掛ける係数。 */
	public static double attrMulFor(double mul) {
		return Math.sqrt(Math.max(0.0, mul));
	}

	public String getBrawlerId() {
		return brawlerId;
	}

	public String getBotName() {
		return botName;
	}

	public static BrawlBot spawn(ServerLevel lvl, Vec3 pos, String brawlerId, String name) {
		BrawlBot bot = BotEntities.BRAWL_BOT.create(lvl, EntitySpawnReason.COMMAND);
		if (bot == null) {
			return null;
		}
		bot.brawlerId = brawlerId;
		bot.botName = name;
		bot.setCustomName(Component.literal(name));
		bot.setCustomNameVisible(false); // 名前はクライアントのレンダラが描く
		bot.snapTo(pos.x, pos.y, pos.z, lvl.getRandom().nextFloat() * 360f, 0f);
		ItemStack weapon = new ItemStack(Brawlers.weaponItem(brawlerId));
		bot.setItemSlot(EquipmentSlot.MAINHAND, weapon);
		if (!lvl.addFreshEntity(bot)) {
			return null;
		}
		Combat.join(bot, brawlerId);
		BrawlerDef def = Brawlers.get(brawlerId);
		if (def != null) {
			// join が速度を適用する想定だが、ボットの属性は自前でも合わせる
			AttributeInstance speed = bot.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.setBaseValue(speedFor(def.speedTiles() * Tiles.BLOCKS_PER_TILE));
			}
		}
		return bot;
	}

	/** 召喚物（ニタの相棒クマ）を呼ぶ。Combat.joinSummon で召喚主に結び付ける。 */
	public static BrawlBot spawnSummon(ServerLevel lvl, Vec3 pos, String brawlerId, String name, LivingEntity owner) {
		BrawlBot bot = BotEntities.BRAWL_BOT.create(lvl, EntitySpawnReason.COMMAND);
		if (bot == null) {
			return null;
		}
		bot.brawlerId = brawlerId;
		bot.botName = name;
		bot.setCustomName(Component.literal(name));
		bot.setCustomNameVisible(false);
		bot.snapTo(pos.x, pos.y, pos.z, owner.getYRot(), 0f);
		if (!lvl.addFreshEntity(bot)) {
			return null;
		}
		Combat.joinSummon(bot, brawlerId, owner.getUUID());
		BrawlerDef def = Brawlers.get(brawlerId);
		if (def != null) {
			AttributeInstance speed = bot.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.setBaseValue(speedFor(def.speedTiles() * Tiles.BLOCKS_PER_TILE));
			}
		}
		return bot;
	}

	// ---- エンティティの設定 ----

	@Override
	protected PathNavigation createNavigation(Level level) {
		// NOTE: Mob のコンストラクタから呼ばれる。このクラスのフィールドは触らない。
		GroundPathNavigation nav = new GroundPathNavigation(this, level);
		nav.setCanFloat(true);
		return nav;
	}

	@Override
	protected void registerGoals() {
		// バニラのAIゴールは使わない
	}

	@Override
	public boolean shouldBeSaved() {
		return false; // ワールド保存時に消える
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public Component getName() {
		return botName.isEmpty() ? super.getName() : Component.literal(botName);
	}

	// ---- 思考 ----

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() || !(level() instanceof ServerLevel lvl)) {
			return;
		}
		BrawlState st = Combat.state(this);
		if (st == null) {
			if (Combat.isDisplay(this)) return; // ロビーの像など見た目だけ
			discard(); // 試合が終わっている
			return;
		}
		if (!st.alive || !isAlive()) {
			getNavigation().stop();
			return;
		}
		setSprinting(false);
		if (isNoAi()) return;
		// スタン・凍結・溜め中（frank のハンマー）は動かない・撃たない（移動速度は core が0にする）
		if (Combat.immobile(this)) {
			getNavigation().stop();
			return;
		}
		if (externalControl) {
			return;
		}
		if (st.ownerId != null) {
			thinkSummon(lvl, st);
			return;
		}
		think(lvl, st);
	}

	// ---- 召喚物（相棒クマ）の思考: 一番近い見える敵を追って噛みつく。居なければ召喚主のそばへ ----

	private int nextChaseTick;

	private void thinkSummon(ServerLevel lvl, BrawlState st) {
		LivingEntity target = null;
		double bestD = BEAR_SIGHT_BLOCKS * BEAR_SIGHT_BLOCKS;
		for (LivingEntity e : Combat.participants(lvl)) {
			if (e == this || !e.isAlive() || Combat.sameSide(this, e) || Combat.isHiddenFrom(e, this)) {
				continue;
			}
			double d = e.distanceToSqr(this);
			if (d < bestD) {
				bestD = d;
				target = e;
			}
		}
		if (target == null) {
			LivingEntity owner = Combat.ownerOf(this);
			if (owner != null && distanceToSqr(owner) > 16.0 && tickCount >= nextChaseTick) {
				nextChaseTick = tickCount + 10;
				getNavigation().moveTo(owner.getX(), owner.getY(), owner.getZ(), 1.0);
			}
			return;
		}
		getLookControl().setLookAt(target);
		boolean reach = getBoundingBox().inflate(BEAR_REACH_BLOCKS, 0.2, BEAR_REACH_BLOCKS).intersects(target.getBoundingBox());
		if (!reach) {
			if (tickCount >= nextChaseTick) {
				nextChaseTick = tickCount + 5;
				if (!getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0)) {
					getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
				}
			}
			return;
		}
		getNavigation().stop();
		if (tickCount >= nextAttackTick) {
			nextAttackTick = tickCount + BEAR_BITE_INTERVAL_TICKS;
			double dx = target.getX() - getX(), dz = target.getZ() - getZ();
			faceYaw((float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG));
			playSound(net.minecraft.sounds.SoundEvents.POLAR_BEAR_WARNING, 0.8f, 1.2f);
			Combat.damage(target, this, BEAR_DMG, false); // 手柄は召喚主（Combat.damage が付け替える）
		}
	}

	private void think(ServerLevel lvl, BrawlState st) {
		BrawlerDef def = Brawlers.get(brawlerId);
		if (def == null) {
			return;
		}
		trackZone(lvl);
		checkStuck();
		double rangeB = Tiles.b(def.attackRangeTiles());
		double searchB = rangeB * 1.2;
		if (st.superCharge >= 1.0f && "edgar,darryl,mortis,lou".contains(brawlerId)) {
			// 突っ込む・跳ぶ・氷の床は、通常攻撃の射程より遠い敵にも使うので、必殺が溜まっていれば広く探す
			searchB = Math.max(searchB, Tiles.b(def.superRangeTiles()) * 0.95);
		}
		LivingEntity target = findTarget(lvl, searchB);
		boolean danger = inDanger(lvl);

		// 狙う相手が切り替わったら反応時間を置く
		int tid = target == null ? -1 : target.getId();
		if (tid != lastTargetId) {
			lastTargetId = tid;
			if (tid != -1) {
				reactUntilTick = tickCount + reactTicks();
			}
		}

		// ① 毒ガスの中 / 5秒以内に飲まれる → 安全地帯の中心へ
		if (danger) {
			AABB zone = Gas.safeZone();
			Vec3 c = zone.getCenter();
			goTo(new Vec3(c.x, getY(), c.z), 1.0, 1L);
			if (target != null) {
				fight(lvl, st, def, target, rangeB, false);
			}
			return;
		}

		// ② HP35%未満で敵が近い → 離れて茂みへ（回復を待つ）
		LivingEntity nearEnemy = nearestEnemy(lvl, 14.0);
		boolean lowHp = st.hp < st.maxHp() * LOW_HP_RATIO;
		if (lowHp && nearEnemy != null) {
			boolean inBush = Combat.inBush(this);
			if (inBush && distanceTo(nearEnemy) > 5.0) {
				getNavigation().stop(); // 茂みで回復待ち
				return;
			}
			if (target != null) {
				fight(lvl, st, def, target, rangeB, false); // 逃げながら撃てるときは撃つ
			}
			if (inBush) {
				return; // 近くに来られたら茂みから動かず撃つだけ
			}
			retreat(lvl, nearEnemy);
			return;
		}

		// ③ 射程×1.2以内に見える敵 → 近づいて左右に動きながら撃つ
		if (target != null) {
			fight(lvl, st, def, target, rangeB, true);
			return;
		}

		// ④ 近くのキューブを拾いに行く
		ItemEntity cube = nearestCube(lvl, 30.0);
		if (cube != null) {
			goTo(cube.position(), 1.0, 2L + cube.getId());
			return;
		}

		// ⑤ 一番近い箱を壊しに行く
		BlockPos box = nearestBox(lvl);
		if (box != null) {
			attackBox(lvl, def, box, rangeB);
			return;
		}

		// ⑥ 中央寄りに歩く
		walkToCenter(lvl);
	}

	// ---- ① 毒ガス ----

	/** 安全地帯の縮む速さを1秒ごとに測る。 */
	private void trackZone(ServerLevel lvl) {
		if (tickCount - lastZoneTick < 20) {
			return;
		}
		AABB z = Gas.safeZone();
		if (lastZone != null && tickCount > lastZoneTick) {
			double dt = (tickCount - lastZoneTick) / 20.0;
			double shrink = Math.max(lastZone.getXsize() - z.getXsize(), lastZone.getZsize() - z.getZsize()) / 2.0;
			zoneShrinkPerSec = Math.max(0.0, shrink / dt);
		}
		lastZone = z;
		lastZoneTick = tickCount;
	}

	/** 今ガスの中、または縁が 5 秒以内（縮む速さ×5秒）に自分の所まで来る。 */
	private boolean inDanger(ServerLevel lvl) {
		if (Gas.inGas(position())) {
			return true;
		}
		AABB zone = Gas.safeZone();
		double margin = zoneShrinkPerSec * GAS_LOOKAHEAD_SEC + 1.0;
		double dx = Math.min(getX() - zone.minX, zone.maxX - getX());
		double dz = Math.min(getZ() - zone.minZ, zone.maxZ - getZ());
		return Math.min(dx, dz) < margin && zoneShrinkPerSec > 0.0;
	}

	// ---- ③ 戦闘 ----

	/** 射程×1.2以内で、茂みに隠れておらず（Combat.isHiddenFrom）、壁に遮られていない一番近い敵。 */
	private LivingEntity findTarget(ServerLevel lvl, double maxDist) {
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		boolean lou = "lou".equals(brawlerId);
		for (LivingEntity e : Combat.participants(lvl)) {
			if (e == this || !e.isAlive() || Combat.sameSide(this, e)) {
				continue;
			}
			double d = e.distanceToSqr(this);
			if (d > maxDist * maxDist) {
				continue;
			}
			if (Combat.isHiddenFrom(e, this)) {
				continue; // 隠れている敵は狙わない
			}
			if (!hasLineOfSight(e)) {
				continue;
			}
			// lou は凍っている・凍結ゲージが溜まっている敵を優先（距離を 0.4 倍に見なす）
			double score = lou && Combat.isFrostMarked(e) ? d * 0.16 : d;
			if (score < bestScore) {
				best = e;
				bestScore = score;
			}
		}
		return best;
	}

	/** 隠れているかどうかに関係なく、近くの敵（退避の判断用。位置を知っているわけではなく、見える敵だけ数える）。 */
	private LivingEntity nearestEnemy(ServerLevel lvl, double maxDist) {
		LivingEntity best = null;
		double bestD = maxDist * maxDist;
		for (LivingEntity e : Combat.participants(lvl)) {
			if (e == this || !e.isAlive() || Combat.sameSide(this, e) || Combat.isHiddenFrom(e, this)) {
				continue;
			}
			double d = e.distanceToSqr(this);
			if (d < bestD) {
				best = e;
				bestD = d;
			}
		}
		return best;
	}

	private void fight(ServerLevel lvl, BrawlState st, BrawlerDef def, LivingEntity target, double rangeB, boolean approach) {
		double dist = distanceTo(target);
		getLookControl().setLookAt(target);
		if (approach) {
			double keep = rangeB * 0.75;
			if (dist > keep) {
				goTo(target.position(), 1.0, 3L + target.getId()); // 射程に入るよう近づく
			} else {
				strafe(target);
			}
		}
		// 反応時間が過ぎてから撃つ
		if (tickCount < reactUntilTick || tickCount < nextAttackTick) {
			return;
		}
		Vec3 aimAt = target.position().add(0, target.getBbHeight() * 0.5, 0);
		double dx = aimAt.x - getX();
		double dz = aimAt.z - getZ();
		float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG) + aimError();
		if (st.superCharge >= 1.0f && dist <= Tiles.b(def.superRangeTiles()) && wantsSuper(st, dist)) {
			if (Brawlers.useSuper(this, yaw)) {
				faceYaw(yaw);
				return;
			}
		}
		if (dist <= rangeB && Brawlers.attack(this, yaw)) {
			faceYaw(yaw);
			nextAttackTick = tickCount + Math.max(1, def.attackCooldownTicks()) + hesitation();
		}
	}

	/** キャラごとの必殺を使う条件（射程内であることは呼ぶ側で確認済み）。 */
	private boolean wantsSuper(BrawlState st, double distBlocks) {
		return switch (brawlerId) {
			case "edgar" -> distBlocks >= 3.0;                  // 敵の近くへ跳ぶ（既に密着なら通常攻撃）
			case "darryl" -> distBlocks >= 3.0;                 // 樽で突っ込む
			case "mortis" -> distBlocks >= 3.0;                 // 通常の突進が届かない距離でコウモリ
			case "lou" -> distBlocks >= Tiles.b(3.0);           // 氷の床は離れた敵に
			case "poco" -> st.hp < st.maxHp() * 0.7 || distBlocks <= Tiles.b(4.0); // 回復したいとき／近いとき
			default -> true;
		};
	}

	/** 撃ったあとのためらい（人間らしく弾を一気に撃ち切らない）。easy 10〜24 / normal 6〜16 / hard 2〜8 tick。 */
	private int hesitation() {
		return switch (difficulty) {
			case "easy" -> 10 + random.nextInt(15);
			case "hard" -> 2 + random.nextInt(7);
			default -> 6 + random.nextInt(11);
		};
	}

	/** 狙いのずれ（度）: easy ±15 / normal ±7 / hard ±3。 */
	private float aimError() {
		double e = switch (difficulty) {
			case "easy" -> 18.0;
			case "hard" -> 6.0;
			default -> 11.0;
		};
		return (float) ((random.nextDouble() * 2.0 - 1.0) * e);
	}

	/** 反応時間: easy 0.8秒 / normal 0.4秒 / hard 0.2秒。 */
	private int reactTicks() {
		return switch (difficulty) {
			case "easy" -> 20;
			case "hard" -> 8;
			default -> 13;
		};
	}

	private void faceYaw(float yaw) {
		setYRot(yaw);
		setYHeadRot(yaw);
		swing(net.minecraft.world.InteractionHand.MAIN_HAND);
	}

	/** 敵に正面を向けたまま左右に動く（壁があれば向きを変える）。経路計算は使わず直接歩かせる。 */
	private void strafe(LivingEntity target) {
		if (tickCount >= strafeFlipTick) {
			strafeDir = random.nextBoolean() ? 1 : -1;
			strafeFlipTick = tickCount + 20 + random.nextInt(30);
		}
		Vec3 to = target.position().subtract(position());
		Vec3 flat = new Vec3(to.x, 0, to.z);
		if (flat.lengthSqr() < 1.0e-4) {
			return;
		}
		Vec3 side = new Vec3(-flat.z, 0, flat.x).normalize();
		for (int i = 0; i < 2; i++) {
			Vec3 dest = position().add(side.scale(strafeDir * 2.0));
			AABB moved = getBoundingBox().move(side.scale(strafeDir * 1.0));
			if (level().noCollision(this, moved)) {
				getNavigation().stop();
				getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, 1.0);
				return;
			}
			strafeDir = -strafeDir; // 壁際なので反対へ
		}
	}

	// ---- ② 退避 ----

	private void retreat(ServerLevel lvl, LivingEntity enemy) {
		if (tickCount - bushScanTick > 40) {
			bushScanTick = tickCount;
			bushGoal = findBush(lvl, enemy);
		}
		if (bushGoal != null) {
			goTo(Vec3.atBottomCenterOf(bushGoal), 1.0, 4L);
			return;
		}
		Vec3 away = position().subtract(enemy.position());
		Vec3 flat = new Vec3(away.x, 0, away.z);
		if (flat.lengthSqr() < 1.0e-4) {
			flat = new Vec3(1, 0, 0);
		}
		Vec3 dest = position().add(flat.normalize().scale(10.0));
		goTo(dest, 1.0, 5L);
	}

	/** 近く(半径10)の茂みの中で、敵から遠くて自分に近いもの。 */
	private BlockPos findBush(ServerLevel lvl, LivingEntity enemy) {
		BlockPos me = blockPosition();
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int dx = -10; dx <= 10; dx++) {
			for (int dz = -10; dz <= 10; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					p.set(me.getX() + dx, me.getY() + dy, me.getZ() + dz);
					if (!lvl.getBlockState(p).is(ArenaBlocks.BUSH)) {
						continue;
					}
					double mine = p.distSqr(me);
					double theirs = enemy.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
					if (theirs < 25.0) {
						continue; // 敵のすぐ近くの茂みは避ける
					}
					double score = mine - theirs * 0.5;
					if (score < bestScore) {
						bestScore = score;
						best = p.immutable();
					}
				}
			}
		}
		return best;
	}

	// ---- ④ キューブ ----

	private ItemEntity nearestCube(ServerLevel lvl, double radius) {
		AABB box = getBoundingBox().inflate(radius);
		ItemEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(ArenaBlocks.POWER_CUBE))) {
			if (isIgnored(2L + ie.getId())) {
				continue;
			}
			double d = ie.distanceToSqr(this);
			if (d < bestD) {
				bestD = d;
				best = ie;
			}
		}
		return best;
	}

	// ---- ⑤ 箱 ----

	/** マップの箱タイルのうち、まだ BOX ブロックが残っている一番近いもの。 */
	private BlockPos nearestBox(ServerLevel lvl) {
		if (boxTiles == null) {
			try {
				ArenaLayout layout = ArenaMap.layout();
				boxTiles = layout == null ? new ArrayList<>() : new ArrayList<>(layout.boxTiles());
			} catch (RuntimeException e) {
				boxTiles = new ArrayList<>();
			}
		}
		BlockPos best = null;
		double bestD = Double.MAX_VALUE;
		for (BlockPos t : boxTiles) {
			if (isIgnored(t.asLong() ^ 0x5bd1e995L)) {
				continue;
			}
			if (!lvl.getBlockState(t).is(ArenaBlocks.BOX)) {
				continue; // 壊れた
			}
			double d = t.distSqr(blockPosition());
			if (d < bestD) {
				bestD = d;
				best = t;
			}
		}
		return best;
	}

	private void attackBox(ServerLevel lvl, BrawlerDef def, BlockPos tile, double rangeB) {
		Vec3 center = new Vec3(tile.getX() + 1.0, tile.getY() + 1.0, tile.getZ() + 1.0); // 2x2x2
		double dist = Math.sqrt(center.distanceToSqr(position()));
		boolean clear = false;
		if (dist <= rangeB * 0.8) {
			Vec3 eye = getEyePosition();
			BlockHitResult hit = lvl.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			clear = hit.getType() == HitResult.Type.MISS
					|| lvl.getBlockState(hit.getBlockPos()).is(ArenaBlocks.BOX);
		}
		if (clear) {
			getNavigation().stop();
			getLookControl().setLookAt(center);
			if (tickCount >= nextAttackTick) {
				float yaw = (float) (Mth.atan2(-(center.x - getX()), center.z - getZ()) * Mth.RAD_TO_DEG);
				if (Brawlers.attack(this, yaw)) {
					faceYaw(yaw);
					nextAttackTick = tickCount + Math.max(1, def.attackCooldownTicks());
				}
			}
		} else {
			goTo(center, 1.0, t(tile));
		}
	}

	private static long t(BlockPos tile) {
		return tile.asLong() ^ 0x5bd1e995L;
	}

	// ---- ⑥ 中央 ----

	private void walkToCenter(ServerLevel lvl) {
		Vec3 center;
		try {
			ArenaLayout layout = ArenaMap.layout();
			center = layout == null ? position() : layout.center();
		} catch (RuntimeException e) {
			center = position();
		}
		if (wander == null || position().distanceToSqr(wander) < 4.0 || isIgnored(6L)) {
			wander = center.add((random.nextDouble() - 0.5) * 24.0, 0, (random.nextDouble() - 0.5) * 24.0);
		}
		goTo(new Vec3(wander.x, getY(), wander.z), 1.0, 6L);
	}

	// ---- 移動・詰まり ----

	/** 経路を使って移動。再計算は1秒に1回まで。goalKey は詰まったとき別の目標へ移るための印。 */
	private void goTo(Vec3 dest, double speedMod, long goalKey) {
		currentGoalKey = goalKey;
		if (tickCount < nextPathTick) {
			return;
		}
		nextPathTick = tickCount + REPATH_TICKS;
		boolean ok = getNavigation().moveTo(dest.x, dest.y, dest.z, speedMod);
		if (!ok) {
			// 経路が見つからなければ直接歩く（壁があれば詰まり検知が別の目標へ切り替える）
			getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, speedMod);
		}
	}

	/** 1秒ごとに位置を比べ、ほとんど動いていなければ今の目標を10秒無視する。 */
	private void checkStuck() {
		if (tickCount - stuckCheckTick < 20) {
			return;
		}
		boolean wantsToMove = currentGoalKey != Long.MIN_VALUE && (getNavigation().isInProgress() || tickCount < nextPathTick + 20);
		double moved = position().distanceTo(stuckPos);
		if (wantsToMove && moved < 0.3 && stuckCheckTick > 0 && !isStandingGoal()) {
			stuckCount++;
			if (stuckCount >= 2) {
				ignored.put(currentGoalKey, tickCount + 200);
				if (currentGoalKey == 6L) {
					wander = null;
				}
				getNavigation().stop();
				nextPathTick = 0; // 別の目標へ向かうので経路を作り直してよい
				stuckCount = 0;
			}
		} else {
			stuckCount = 0;
		}
		stuckPos = position();
		stuckCheckTick = tickCount;
		ignored.values().removeIf(until -> until <= tickCount);
	}

	private int stuckCount;

	/** 退避中の茂みでの待機などは動かなくて当然。 */
	private boolean isStandingGoal() {
		return !getNavigation().isInProgress() && position().distanceToSqr(stuckPos) < 0.09 && Combat.inBush(this);
	}

	private boolean isIgnored(long key) {
		Integer until = ignored.get(key);
		return until != null && until > tickCount;
	}
}
