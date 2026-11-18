package com.amaguri.brawl.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.arena.Arena;
import com.amaguri.brawl.arena.ArenaMap;
import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.mode.Boxes;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 試合参加者（プレイヤーもボットも）のHP・弾・ゲージ・被弾・同期。 */
public final class Combat {
	private Combat() {}

	/** プレイヤー基本の移動速度属性値と、そのときのブロック/秒。 */
	private static final double PLAYER_BASE_SPEED = 0.1;
	private static final double PLAYER_BASE_BPS = 4.317;
	private static final double BUSH_REVEAL_BLOCKS = 4.0;
	private static final double BARS_RANGE = 48.0;

	private static final Map<UUID, BrawlState> STATES = new HashMap<>();
	private static final Map<UUID, Double> ORIGINAL_SPEED = new HashMap<>();
	/** 毎tick更新する UUID→実体 */
	private static final Map<UUID, LivingEntity> ENTITIES = new HashMap<>();
	private static long tick = 0;
	private static boolean inited = false;

	/** 現在のtick（core 独自カウンタ）。lastActTick などと比べる用。 */
	public static long now() {
		return tick;
	}

	public static void init() {
		if (inited) return;
		inited = true;
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !STATES.containsKey(entity.getUUID()));
		ServerTickEvents.END_SERVER_TICK.register(Combat::onTick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer p = handler.getPlayer();
			// 状態は消さない（試合側 mode が DISCONNECT でキューブを落としてから Combat.leave する）
			BrawlState st = STATES.get(p.getUUID());
			if (st != null) restoreSpeed(p, null);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			STATES.clear();
			ORIGINAL_SPEED.clear();
			ENTITIES.clear();
			Projectiles.clear();
		});
	}

	// ---------------------------------------------------------------- 参加・退出

	public static BrawlState join(LivingEntity e, String brawlerId) {
		BrawlerDef def = Brawlers.get(brawlerId);
		BrawlState st = new BrawlState(e.getUUID(), brawlerId);
		st.hp = st.maxHp();
		st.ammo = def != null ? def.maxAmmo() : 3;
		st.superCharge = 0;
		st.cubes = 0;
		st.alive = true;
		st.lastActTick = tick;
		st.lastHurtTick = tick;
		st.revealUntilTick = 0;
		STATES.put(e.getUUID(), st);
		ENTITIES.put(e.getUUID(), e);

		AttributeInstance sp = e.getAttribute(Attributes.MOVEMENT_SPEED);
		if (sp != null) {
			ORIGINAL_SPEED.putIfAbsent(e.getUUID(), sp.getBaseValue());
			double bps = (def != null ? def.speedTiles() : 3.0) * 2.0;
			if (e instanceof BrawlBot) {
				sp.setBaseValue(BrawlBot.speedFor(bps));
			} else {
				sp.setBaseValue(bps / PLAYER_BASE_BPS * PLAYER_BASE_SPEED);
			}
		}
		if (e instanceof ServerPlayer p) {
			p.getFoodData().setFoodLevel(6);
			p.getFoodData().setSaturation(0f);
		}
		e.setSprinting(false);
		return st;
	}

	/** 召喚物（相棒）の参加。join と同じで、召喚主の UUID を記録する。召喚主と相棒は互いに当たらない。 */
	public static BrawlState joinSummon(LivingEntity e, String brawlerId, UUID ownerId) {
		BrawlState st = join(e, brawlerId);
		st.ownerId = ownerId;
		return st;
	}

	/** 召喚主の実体。召喚物でなければ（または居なければ）null。 */
	public static LivingEntity ownerOf(Entity e) {
		BrawlState st = state(e);
		if (st == null || st.ownerId == null) return null;
		LivingEntity o = ENTITIES.get(st.ownerId);
		return o != null && !o.isRemoved() ? o : null;
	}

	private static UUID rootId(Entity e) {
		BrawlState st = state(e);
		return st != null && st.ownerId != null ? st.ownerId : e.getUUID();
	}

	/** 同じ陣営（召喚主と相棒、同じ主の相棒どうし）。別々の実体のときだけ true。 */
	public static boolean sameSide(Entity a, Entity b) {
		if (a == null || b == null || a == b) return false;
		return rootId(a).equals(rootId(b));
	}

	/** その召喚主の召喚物をすべて消す（2体目を呼ぶとき・召喚主が倒れたとき）。 */
	public static void dismissSummons(UUID ownerId) {
		for (Map.Entry<UUID, BrawlState> en : new ArrayList<>(STATES.entrySet())) {
			if (ownerId.equals(en.getValue().ownerId)) removeSummon(en.getKey());
		}
	}

	private static void removeSummon(UUID id) {
		LivingEntity e = ENTITIES.get(id);
		STATES.remove(id);
		ENTITIES.remove(id);
		ORIGINAL_SPEED.remove(id);
		if (e != null && !e.isRemoved()) e.discard();
	}

	public static void leave(LivingEntity e) {
		BrawlState st = STATES.remove(e.getUUID());
		ENTITIES.remove(e.getUUID());
		restoreSpeed(e, st);
		if (e instanceof ServerPlayer p && st != null) {
			p.getFoodData().setFoodLevel(20);
			p.getFoodData().setSaturation(5f);
			sendCleared(p);
		}
	}

	private static void restoreSpeed(LivingEntity e, BrawlState ignored) {
		Double orig = ORIGINAL_SPEED.remove(e.getUUID());
		AttributeInstance sp = e.getAttribute(Attributes.MOVEMENT_SPEED);
		if (sp != null && orig != null) sp.setBaseValue(orig);
	}

	private static void sendCleared(ServerPlayer p) {
		// 試合終了：HUD を消す（Hud 1回）。隠れ情報も残らないよう空を1回送る。
		ServerPlayNetworking.send(p, new Net.HudPayload("", 0, 0, 0f, 0, 0f, 0, false));
		ServerPlayNetworking.send(p, new Net.HiddenPayload(new int[0]));
	}

	public static BrawlState state(Entity e) {
		return e == null ? null : STATES.get(e.getUUID());
	}

	public static List<LivingEntity> participants(ServerLevel lvl) {
		List<LivingEntity> out = new ArrayList<>();
		for (BrawlState st : STATES.values()) {
			if (!st.alive) continue;
			LivingEntity e = resolve(lvl.getServer(), st.id);
			if (e != null && !e.isRemoved() && e.level() == lvl) out.add(e);
		}
		return out;
	}

	public static void clearAll(MinecraftServer s) {
		for (UUID id : new ArrayList<>(STATES.keySet())) {
			LivingEntity e = resolve(s, id);
			if (e != null) {
				leave(e);
			} else {
				STATES.remove(id);
				ORIGINAL_SPEED.remove(id);
				ENTITIES.remove(id);
			}
		}
		STATES.clear();
		ENTITIES.clear();
		Projectiles.clear();
	}

	private static LivingEntity resolve(MinecraftServer server, UUID id) {
		LivingEntity cached = ENTITIES.get(id);
		if (cached != null && !cached.isRemoved()) return cached;
		LivingEntity found = null;
		if (server != null) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) {
				found = p;
			} else {
				for (ServerLevel l : server.getAllLevels()) {
					Entity en = l.getEntity(id);
					if (en instanceof LivingEntity le) {
						found = le;
						break;
					}
				}
			}
		}
		if (found != null) ENTITIES.put(id, found); else ENTITIES.remove(id);
		return found;
	}

	// ---------------------------------------------------------------- ダメージ・回復

	public static void damage(LivingEntity victim, LivingEntity attacker, int baseDamage, boolean fromMainAttack) {
		BrawlState vs = state(victim);
		if (vs == null || !vs.alive) return;
		if (attacker != null && sameSide(attacker, victim)) return; // 召喚主と相棒は当たらない
		BrawlState as = attacker != null ? state(attacker) : null;
		// 召喚物の攻撃は召喚主の手柄（ゲージは溜めない）
		LivingEntity credit = attacker;
		if (as != null && as.ownerId != null) {
			credit = ownerOf(attacker);
			fromMainAttack = false;
		}
		if (!MatchHooks.canHit.test(credit, victim)) return;
		float mul = as != null ? as.damageMul() : 1.0f;
		int dmg = Math.round(baseDamage * mul);
		if (dmg <= 0) return;

		vs.hp -= dmg;
		vs.lastHurtTick = tick;

		// 被弾演出：赤く光る＋被弾音（ダメージイベントのパケットでクライアントが hurt アニメと音を出す）
		if (victim.level() instanceof ServerLevel sl) {
			sl.broadcastDamageEvent(victim, sl.damageSources().generic());
		}

		if (fromMainAttack && as != null && attacker != victim) { // 召喚物は上で false にしてある
			BrawlerDef d = Brawlers.get(as.brawlerId);
			if (d != null) as.superCharge = Math.min(1.0f, as.superCharge + d.superPerHit());
		}

		if (vs.hp <= 0) {
			vs.hp = 0;
			if (vs.alive) {
				vs.alive = false;
				MatchHooks.onEliminated.accept(victim, credit);
			}
		}
	}

	public static void heal(LivingEntity e, int amount) {
		BrawlState st = state(e);
		if (st == null || !st.alive || amount <= 0) return;
		st.hp = Math.min(st.maxHp(), st.hp + amount);
	}

	public static void addCube(LivingEntity e) {
		BrawlState st = state(e);
		if (st == null || !st.alive || st.ownerId != null) return; // 召喚物は拾えない
		st.cubes++;
		st.hp = Math.min(st.maxHp(), st.hp + 400);
	}

	public static void knockback(LivingEntity e, Vec3 dirUnit, double blocks) {
		if (state(e) == null) return;
		double len = Math.sqrt(dirUnit.x * dirUnit.x + dirUnit.z * dirUnit.z);
		if (len < 1e-6 || blocks <= 0) return;
		// 地面の摩擦で 初速の約2.2倍 滑る
		double v = blocks / 2.2;
		Vec3 cur = e.getDeltaMovement();
		e.setDeltaMovement(dirUnit.x / len * v, Math.max(cur.y, 0.0), dirUnit.z / len * v);
		e.hurtMarked = true;
	}

	public static void areaDamage(ServerLevel lvl, LivingEntity attacker, Vec3 center, double radiusBlocks,
			int baseDamage, boolean fromMainAttack) {
		double r2 = radiusBlocks * radiusBlocks;
		for (LivingEntity t : participants(lvl)) {
			if (t == attacker || sameSide(attacker, t)) continue;
			if (distSqToBox(center, t.getBoundingBox()) <= r2) {
				damage(t, attacker, baseDamage, fromMainAttack);
			}
		}
		// 箱
		try {
			ArenaLayout layout = ArenaMap.layout();
			if (layout != null) {
				for (BlockPos bp : layout.boxTiles()) {
					AABB box = new AABB(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 2, bp.getY() + 2, bp.getZ() + 2);
					if (distSqToBox(center, box) <= r2) {
						BlockState bs = lvl.getBlockState(bp);
						if (Arena.isBulletSolid(bs)) Boxes.damage(lvl, bp, baseDamage, attacker);
					}
				}
			}
		} catch (RuntimeException ex) {
			// マップ未読み込みなど。箱ダメージは諦める
		}
	}

	private static double distSqToBox(Vec3 p, AABB b) {
		double dx = Math.max(Math.max(b.minX - p.x, 0), p.x - b.maxX);
		double dy = Math.max(Math.max(b.minY - p.y, 0), p.y - b.maxY);
		double dz = Math.max(Math.max(b.minZ - p.z, 0), p.z - b.maxZ);
		return dx * dx + dy * dy + dz * dz;
	}

	// ---------------------------------------------------------------- 弾・ゲージ

	public static boolean consumeAmmo(LivingEntity e) {
		BrawlState st = state(e);
		if (st == null || !st.alive || st.ammo < 0.999f) return false;
		st.ammo = Math.max(0f, st.ammo - 1f);
		st.lastActTick = tick;
		st.revealUntilTick = Math.max(st.revealUntilTick, tick + 20);
		return true;
	}

	public static boolean consumeSuper(LivingEntity e) {
		BrawlState st = state(e);
		if (st == null || !st.alive || st.superCharge < 0.999f) return false;
		st.superCharge = 0f;
		st.lastActTick = tick;
		st.revealUntilTick = Math.max(st.revealUntilTick, tick + 20);
		return true;
	}

	// ---------------------------------------------------------------- 茂み

	public static boolean inBush(LivingEntity e) {
		Level lvl = e.level();
		BlockState feet = lvl.getBlockState(BlockPos.containing(e.getX(), e.getY(), e.getZ()));
		if (Arena.isBush(feet)) return true;
		BlockState head = lvl.getBlockState(BlockPos.containing(e.getX(), e.getEyeY(), e.getZ()));
		return Arena.isBush(head);
	}

	public static boolean isHiddenFrom(LivingEntity target, LivingEntity viewer) {
		if (target == viewer) return false;
		BrawlState ts = state(target);
		if (ts == null) return false;
		if (tick <= ts.revealUntilTick) return false;
		if (target.distanceTo(viewer) <= BUSH_REVEAL_BLOCKS) return false;
		return inBush(target);
	}

	// ---------------------------------------------------------------- tick

	private static void onTick(MinecraftServer server) {
		tick++;

		// 実体の更新・消えた参加者の掃除
		Iterator<Map.Entry<UUID, BrawlState>> it = STATES.entrySet().iterator();
		List<LivingEntity> all = new ArrayList<>();
		while (it.hasNext()) {
			Map.Entry<UUID, BrawlState> en = it.next();
			LivingEntity e = resolve(server, en.getKey());
			if (e == null || e.isRemoved()) {
				it.remove();
				ORIGINAL_SPEED.remove(en.getKey());
				ENTITIES.remove(en.getKey());
				continue;
			}
			all.add(e);
		}

		// 召喚主が倒れた・居なくなった召喚物は消す
		List<UUID> orphans = new ArrayList<>();
		for (LivingEntity e : all) {
			BrawlState st = STATES.get(e.getUUID());
			if (st == null || st.ownerId == null) continue;
			BrawlState os = STATES.get(st.ownerId);
			if (os == null || !os.alive || !st.alive) orphans.add(e.getUUID());
		}
		for (UUID id : orphans) removeSummon(id);
		all.removeIf(e -> !STATES.containsKey(e.getUUID()));

		for (LivingEntity e : all) {
			BrawlState st = STATES.get(e.getUUID());
			if (st == null || !st.alive) continue;
			tickParticipant(e, st);
		}

		Projectiles.tick();

		if ((tick & 1) == 0) syncHudAndBars(server, all);
		if ((tick & 3) == 0) syncHidden(server, all);
		if (tick % 20 == 0) syncLooks(server);
	}

	private static void tickParticipant(LivingEntity e, BrawlState st) {
		BrawlerDef def = Brawlers.get(st.brawlerId);
		int maxAmmo = def != null ? def.maxAmmo() : 3;
		double reloadSec = def != null ? def.reloadSec() : 1.5;

		// 弾の回復
		if (st.ammo < maxAmmo) {
			st.ammo = Math.min(maxAmmo, st.ammo + (float) (1.0 / Math.max(1.0, reloadSec * 20.0)));
		}

		// 自然回復：3秒間 攻撃も被弾もしなければ 毎秒 最大HPの13%
		long since = tick - Math.max(st.lastActTick, st.lastHurtTick);
		if (since >= 60 && (since - 60) % 20 == 0 && st.hp < st.maxHp()) {
			heal(e, Math.round(st.maxHp() * 0.13f));
		}

		// 走り不可・空腹固定
		e.setSprinting(false);
		if (e instanceof ServerPlayer p) {
			p.getFoodData().setFoodLevel(6);
			p.getFoodData().setSaturation(0f);
		}
	}

	/** 参加者ではない「見た目だけ」の実体（ロビーの像など）。LooksPayload に載る。 */
	private static final Map<Integer, String> DISPLAY = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Map<Integer, LivingEntity> DISPLAY_E = new java.util.concurrent.ConcurrentHashMap<>();

	public static void setDisplay(LivingEntity e, String brawlerId) {
		DISPLAY.put(e.getId(), brawlerId);
		DISPLAY_E.put(e.getId(), e);
	}

	public static void clearDisplay(LivingEntity e) {
		DISPLAY.remove(e.getId());
		DISPLAY_E.remove(e.getId());
	}

	public static boolean isDisplay(Entity e) {
		return DISPLAY.containsKey(e.getId());
	}

	/** 全員（観戦者も）に「どの実体がどのキャラの見た目か」を送る。試合外は空。 */
	private static void syncLooks(MinecraftServer server) {
		List<LivingEntity> es = new ArrayList<>();
		for (Map.Entry<java.util.UUID, BrawlState> en : STATES.entrySet()) {
			LivingEntity e = ENTITIES.get(en.getKey());
			if (e != null && !e.isRemoved()) es.add(e);
		}
		DISPLAY_E.values().removeIf(e -> e.isRemoved());
		DISPLAY.keySet().retainAll(DISPLAY_E.keySet());
		int n = es.size() + DISPLAY.size();
		int[] ids = new int[n];
		String[] bs = new String[n];
		for (int i = 0; i < es.size(); i++) {
			ids[i] = es.get(i).getId();
			bs[i] = STATES.get(es.get(i).getUUID()).brawlerId;
		}
		int i = es.size();
		for (Map.Entry<Integer, String> d : DISPLAY.entrySet()) {
			ids[i] = d.getKey();
			bs[i++] = d.getValue();
		}
		Net.LooksPayload pl = new Net.LooksPayload(ids, bs);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, pl);
	}

	private static void syncHudAndBars(MinecraftServer server, List<LivingEntity> all) {
		for (LivingEntity v : all) {
			if (!(v instanceof ServerPlayer p)) continue;
			BrawlState st = STATES.get(p.getUUID());
			if (st == null) continue;
			BrawlerDef def = Brawlers.get(st.brawlerId);
			ServerPlayNetworking.send(p, new Net.HudPayload(st.brawlerId, st.hp, st.maxHp(), st.ammo,
					def != null ? def.maxAmmo() : 3, st.superCharge, st.cubes, st.alive));

			// 頭上HPバー：48ブロック以内の alive 参加者（隠れている者は除く）
			List<LivingEntity> shown = new ArrayList<>();
			for (LivingEntity t : all) {
				BrawlState ts = STATES.get(t.getUUID());
				if (ts == null || !ts.alive || t.level() != p.level()) continue;
				if (t.distanceToSqr(p) > BARS_RANGE * BARS_RANGE) continue;
				if (st.alive && isHiddenFrom(t, p)) continue;
				shown.add(t);
			}
			int n = shown.size();
			int[] ids = new int[n], hp = new int[n], maxHp = new int[n], team = new int[n], cubes = new int[n];
			for (int i = 0; i < n; i++) {
				LivingEntity t = shown.get(i);
				BrawlState ts = STATES.get(t.getUUID());
				ids[i] = t.getId();
				hp[i] = ts.hp;
				maxHp[i] = ts.maxHp();
				team[i] = t == p ? 0 : (ts.ownerId != null && ts.ownerId.equals(p.getUUID()) ? 2 : 1);
				cubes[i] = ts.cubes;
			}
			ServerPlayNetworking.send(p, new Net.BarsPayload(ids, hp, maxHp, team, cubes));
		}
	}

	private static void syncHidden(MinecraftServer server, List<LivingEntity> all) {
		for (LivingEntity v : all) {
			if (!(v instanceof ServerPlayer p)) continue;
			BrawlState st = STATES.get(p.getUUID());
			if (st == null) continue;
			List<Integer> hidden = new ArrayList<>();
			if (st.alive) {
				for (LivingEntity t : all) {
					if (t == p) continue;
					BrawlState ts = STATES.get(t.getUUID());
					if (ts == null || !ts.alive || t.level() != p.level()) continue;
					if (isHiddenFrom(t, p)) hidden.add(t.getId());
				}
			}
			int[] arr = new int[hidden.size()];
			for (int i = 0; i < arr.length; i++) arr[i] = hidden.get(i);
			ServerPlayNetworking.send(p, new Net.HiddenPayload(arr));
		}
	}
}
