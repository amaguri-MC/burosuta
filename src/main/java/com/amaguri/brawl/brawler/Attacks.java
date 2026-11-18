package com.amaguri.brawl.brawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.api.Tiles;
import com.amaguri.brawl.arena.Arena;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;
import com.amaguri.brawl.core.Projectiles;
import com.amaguri.brawl.core.Shot;

import static com.amaguri.brawl.brawler.BrawlerStats.*;

/** 通常攻撃・必殺の中身。プレイヤーもボットも LivingEntity として同じ処理。数値は BrawlerStats のみから読む。 */
final class Attacks {
	private Attacks() {}

	private static final ParticleOptions ORANGE_SPARK = new DustParticleOptions(0xFF8A1E, 0.9f);
	private static final ParticleOptions ORANGE_SPARK_BIG = new DustParticleOptions(0xFF6A10, 1.3f);
	private static final ParticleOptions YELLOW_BEAM = new DustParticleOptions(0xFFE82E, 0.8f);

	private static final ParticleOptions WAVE_DUST = new DustParticleOptions(0xD8ECFF, 1.2f);
	private static final ParticleOptions POTION_DUST = new DustParticleOptions(0x7A3FA0, 1.0f);
	private static final ParticleOptions POISON_DUST = new DustParticleOptions(0x4CD84C, 1.4f);

	// ---------- 共通 ----------

	/** yaw（度）→ 水平の単位ベクトル。Minecraft は yaw=0 が +Z（南）。 */
	static Vec3 dir(double yawDeg) {
		double r = Math.toRadians(yawDeg);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private static boolean active(LivingEntity e) {
		BrawlState st = Combat.state(e);
		return st != null && st.alive && !e.isRemoved();
	}

	/** 弾の発射位置: 足元+1.0、少し前。 */
	private static Vec3 muzzle(LivingEntity u, Vec3 d) {
		return new Vec3(u.getX(), u.getY() + 1.0, u.getZ()).add(d.scale(0.5));
	}

	private static void sound(ServerLevel lvl, Vec3 p, SoundEvent s, float vol, float pitch) {
		lvl.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
	}

	private static void shoot(ServerLevel lvl, LivingEntity u, double yaw, double speed, double rangeTiles, double radius,
			int dmg, boolean main, boolean breaks, ParticleOptions trail, Shot.OnHit onHit, Shot.OnEnd onEnd) {
		Vec3 d = dir(yaw);
		Projectiles.spawn(lvl, new Shot(u, muzzle(u, d), d, speed, Tiles.b(rangeTiles), radius, dmg, main, breaks, false,
				trail, onHit, onEnd));
	}

	/** 扇状に n 発（等間隔＋少しランダム）。 */
	private static void fan(ServerLevel lvl, LivingEntity u, float yaw, int n, double fanDeg, double speed, double rangeTiles,
			double radius, int dmg, boolean main, boolean breaks, ParticleOptions trail, Shot.OnHit onHit) {
		double step = n > 1 ? fanDeg / (n - 1) : 0;
		for (int i = 0; i < n; i++) {
			double off = n > 1 ? -fanDeg / 2 + step * i : 0;
			off += (lvl.getRandom().nextDouble() - 0.5) * step * 0.4;
			shoot(lvl, u, yaw + off, speed, rangeTiles, radius, dmg, main, breaks, trail, onHit, null);
		}
	}

	/** 壁（WALL タイル）を中心の周囲 radius ブロックで壊す。Arena.breakTile は壊せないものでは false を返すだけ。 */
	private static void breakWallsAround(ServerLevel lvl, Vec3 c, double radius) {
		for (double x = -radius; x <= radius + 1e-6; x += 1.0) {
			for (double z = -radius; z <= radius + 1e-6; z += 1.0) {
				if (x * x + z * z > (radius + 0.75) * (radius + 0.75)) continue;
				Arena.breakTile(lvl, BlockPos.containing(c.x + x, c.y + 0.5, c.z + z));
				Arena.breakTile(lvl, BlockPos.containing(c.x + x, c.y + 1.5, c.z + z));
			}
		}
	}

	private static void moveTo(LivingEntity e, double x, double y, double z) {
		if (e instanceof ServerPlayer sp) sp.teleportTo(x, y, z);
		else e.snapTo(x, y, z);
		e.setDeltaMovement(Vec3.ZERO);
		e.resetFallDistance();
	}

	// ---------- 通常攻撃 ----------

	static void normal(ServerLevel lvl, LivingEntity u, String id, float yaw) {
		Vec3 d = dir(yaw);
		Vec3 m = muzzle(u, d);
		switch (id) {
			case "rusty" -> {
				muzzleFlash(lvl, m, d, ParticleTypes.FLAME);
				sound(lvl, m, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);
				fan(lvl, u, yaw, RUSTY_PELLETS, RUSTY_FAN, RUSTY_SPEED_BPT, RUSTY_RANGE, RUSTY_PELLET_RADIUS,
						RUSTY_DMG, true, false, ORANGE_SPARK, null);
			}
			case "kaden" -> {
				sound(lvl, m, SoundEvents.CROSSBOW_SHOOT, 0.7f, 1.6f);
				for (int i = 0; i < KADEN_BULLETS; i++) {
					Tasks.after(i * KADEN_BULLET_INTERVAL_TICKS, () -> coltBullet(lvl, u, yaw, KADEN_DMG, KADEN_RANGE, true, false));
				}
			}
			case "gouen" -> {
				muzzleFlash(lvl, m, d, ParticleTypes.FLAME);
				sound(lvl, m, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
				fan(lvl, u, yaw, GOUEN_PELLETS, GOUEN_FAN, GOUEN_SPEED_BPT, GOUEN_RANGE, GOUEN_PELLET_RADIUS,
						GOUEN_DMG, true, false, ORANGE_SPARK, null);
			}
			case "mimosa" -> {
				lvl.sendParticles(ParticleTypes.SMOKE, m.x, m.y, m.z, 8, 0.15, 0.15, 0.15, 0.02);
				sound(lvl, m, SoundEvents.FIREWORK_ROCKET_SHOOT, 1.0f, 0.7f);
				shoot(lvl, u, yaw, MIMOSA_SPEED_BPT, MIMOSA_RANGE, MIMOSA_ROCKET_RADIUS, 0, true, false, ParticleTypes.SMOKE,
						(l, s, t, p) -> {}, // 命中時のダメージは OnEnd の範囲ダメージに任せる
						(l, s, p, wall) -> explode(l, u, p, MIMOSA_DMG, true, false));
			}
			case "takomaru" -> {
				Vec3 f = m.add(d.scale(1.0));
				lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, f.x, f.y, f.z, 1, 0, 0, 0, 0);
				sound(lvl, m, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
				fan(lvl, u, yaw, TAKOMARU_PUNCHES, TAKOMARU_FAN, TAKOMARU_SPEED_BPT, TAKOMARU_RANGE, TAKOMARU_PUNCH_RADIUS,
						TAKOMARU_DMG, true, false, ParticleTypes.CRIT, null);
			}
			case "nita" -> {
				Vec3 f = m.add(d.scale(0.8));
				lvl.sendParticles(ParticleTypes.POOF, f.x, f.y, f.z, 8, 0.3, 0.2, 0.3, 0.05);
				sound(lvl, m, SoundEvents.POLAR_BEAR_WARNING, 0.9f, 1.0f);
				// 貫通する衝撃波（前へまっすぐ）
				Projectiles.spawn(lvl, new Shot(u, m, d, NITA_WAVE_SPEED_BPT, Tiles.b(NITA_RANGE), NITA_WAVE_RADIUS, NITA_DMG,
						true, false, true, WAVE_DUST, null, null));
			}
			case "barley" -> {
				sound(lvl, m, SoundEvents.WITCH_THROW, 0.9f, 1.0f);
				Vec3 land = u.position().add(d.scale(lobDistance(lvl, u, d)));
				lobPotion(lvl, u, land, true, BARLEY_PUDDLE_TICKS);
			}
			default -> {}
		}
	}

	private static void muzzleFlash(ServerLevel lvl, Vec3 m, Vec3 d, ParticleOptions p) {
		Vec3 f = m.add(d.scale(0.4));
		lvl.sendParticles(p, f.x, f.y, f.z, 6, 0.15, 0.1, 0.15, 0.03);
		lvl.sendParticles(ParticleTypes.CRIT, f.x, f.y, f.z, 6, 0.2, 0.15, 0.2, 0.2);
	}

	private static void coltBullet(ServerLevel lvl, LivingEntity u, float yaw, int dmg, double rangeTiles, boolean main, boolean breaks) {
		if (!active(u)) return;
		double off = (lvl.getRandom().nextDouble() * 2 - 1) * KADEN_SCATTER_DEG;
		Vec3 m = muzzle(u, dir(yaw));
		sound(lvl, m, SoundEvents.DISPENSER_LAUNCH, 0.5f, 1.8f);
		shoot(lvl, u, yaw + off, KADEN_SPEED_BPT, rangeTiles, KADEN_BULLET_RADIUS, dmg, main, breaks, YELLOW_BEAM, null, null);
	}

	/** ロケットの爆発（範囲ダメージ＋煙・爆発音）。 */
	private static void explode(ServerLevel lvl, LivingEntity owner, Vec3 p, int dmg, boolean main, boolean breaks) {
		lvl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		lvl.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 10, 0.5, 0.3, 0.5, 0.02);
		lvl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 12, 0.6, 0.3, 0.6, 0.05);
		sound(lvl, p, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.1f);
		Combat.areaDamage(lvl, owner, p, Tiles.b(MIMOSA_BLAST_TILES), dmg, main);
		if (breaks) breakWallsAround(lvl, p, Tiles.b(MIMOSA_BLAST_TILES));
	}

	// ---------- 必殺 ----------

	static void superMove(ServerLevel lvl, LivingEntity u, String id, float yaw) {
		Vec3 d = dir(yaw);
		Vec3 m = muzzle(u, d);
		switch (id) {
			case "rusty" -> {
				muzzleFlash(lvl, m, d, ParticleTypes.FLAME);
				sound(lvl, m, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.6f);
				fan(lvl, u, yaw, RUSTY_SUPER_PELLETS, RUSTY_SUPER_FAN, RUSTY_SPEED_BPT, RUSTY_SUPER_RANGE,
						RUSTY_PELLET_RADIUS, RUSTY_SUPER_DMG, false, true, ORANGE_SPARK_BIG,
						(l, s, t, p) -> {
							Combat.damage(t, s.owner(), s.damage(), false);
							Combat.knockback(t, s.dir(), Tiles.b(RUSTY_SUPER_KNOCK_TILES));
						});
			}
			case "kaden" -> {
				sound(lvl, m, SoundEvents.CROSSBOW_SHOOT, 0.9f, 1.3f);
				for (int i = 0; i < KADEN_SUPER_BULLETS; i++) {
					Tasks.after(i * KADEN_BULLET_INTERVAL_TICKS,
							() -> coltBullet(lvl, u, yaw, KADEN_SUPER_DMG, KADEN_SUPER_RANGE, false, true));
				}
			}
			case "gouen" -> bullDash(lvl, u, d);
			case "mimosa" -> brockRain(lvl, u, d);
			case "takomaru" -> elprimoJump(lvl, u, d);
			case "nita" -> summonBear(lvl, u, d);
			case "barley" -> potionRain(lvl, u, d);
			default -> {}
		}
	}

	private static void bullDash(ServerLevel lvl, LivingEntity u, Vec3 d) {
		double perTick = Tiles.b(GOUEN_SUPER_DASH_TILES) / GOUEN_SUPER_DASH_TICKS;
		Set<LivingEntity> hit = new HashSet<>();
		int[] t = {0};
		sound(lvl, u.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.6f);
		Tasks.add(() -> {
			if (!active(u) || t[0]++ >= GOUEN_SUPER_DASH_TICKS) return true;
			Vec3 p = u.position();
			u.setDeltaMovement(d.x * perTick, u.getDeltaMovement().y, d.z * perTick);
			u.hurtMarked = true; // プレイヤーにも速度を同期
			lvl.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.2, p.z, 3, 0.3, 0.1, 0.3, 0.02);
			lvl.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1.0, p.z, 2, 0.3, 0.3, 0.3, 0.1);
			// 進行方向の壁を壊す（弾ではないので自前で）
			for (double k = 1.5; k <= 3.0; k += 1.5) {
				Vec3 q = p.add(d.scale(k));
				Arena.breakTile(lvl, BlockPos.containing(q.x, q.y + 0.5, q.z));
				Arena.breakTile(lvl, BlockPos.containing(q.x, q.y + 1.5, q.z));
			}
			double r2 = GOUEN_SUPER_HIT_RADIUS_BLOCKS * GOUEN_SUPER_HIT_RADIUS_BLOCKS;
			for (LivingEntity e : new ArrayList<>(Combat.participants(lvl))) {
				if (e == u || Combat.sameSide(u, e) || hit.contains(e) || e.position().distanceToSqr(p) > r2) continue;
				hit.add(e);
				Combat.damage(e, u, GOUEN_SUPER_DMG, false);
				Combat.knockback(e, d, Tiles.b(GOUEN_SUPER_KNOCK_TILES));
				sound(lvl, e.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
			}
			return false;
		});
	}

	private static void brockRain(ServerLevel lvl, LivingEntity u, Vec3 d) {
		Vec3 center = u.position().add(d.scale(Tiles.b(MIMOSA_SUPER_CENTER_TILES)));
		double area = Tiles.b(MIMOSA_SUPER_AREA_TILES);
		sound(lvl, u.position(), SoundEvents.FIREWORK_ROCKET_SHOOT, 1.0f, 0.5f);
		for (int i = 0; i < MIMOSA_SUPER_ROCKETS; i++) {
			// 円内に一様ランダム
			double ang = lvl.getRandom().nextDouble() * Math.PI * 2;
			double rad = Math.sqrt(lvl.getRandom().nextDouble()) * area;
			Vec3 target = new Vec3(center.x + Math.cos(ang) * rad, center.y, center.z + Math.sin(ang) * rad);
			Tasks.after(i * MIMOSA_SUPER_INTERVAL_TICKS, () -> {
				if (!active(u)) return;
				int[] fall = {MIMOSA_SUPER_FALL_TICKS};
				Tasks.add(() -> {
					int left = --fall[0];
					double h = left * 1.5;
					lvl.sendParticles(ParticleTypes.SMOKE, target.x, target.y + h, target.z, 3, 0.1, 0.3, 0.1, 0.01);
					lvl.sendParticles(ParticleTypes.CRIT, target.x, target.y + 0.1, target.z, 2, Tiles.b(MIMOSA_BLAST_TILES) * 0.5, 0, Tiles.b(MIMOSA_BLAST_TILES) * 0.5, 0.0);
					if (left > 0) return false;
					// 撃った本人が脱落していても着弾はさせる（owner は null にせず渡す）
					explode(lvl, u, target, MIMOSA_SUPER_DMG, false, true);
					return true;
				});
			});
		}
	}

	private static void elprimoJump(ServerLevel lvl, LivingEntity u, Vec3 d) {
		Vec3 start = u.position();
		// 着地点: 最大6タイル先。壁の中なら手前に戻す
		double dist = Tiles.b(TAKOMARU_SUPER_JUMP_TILES);
		Vec3 end = start.add(d.scale(dist));
		while (dist > 0 && blocked(lvl, end)) {
			dist -= 0.5;
			end = start.add(d.scale(dist));
		}
		if (dist <= 0) end = start;
		final Vec3 land = end;
		int T = TAKOMARU_SUPER_FLIGHT_TICKS;
		int[] t = {0};
		sound(lvl, start, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.6f);
		Tasks.add(() -> {
			if (!active(u)) return true;
			t[0]++;
			double f = Math.min(1.0, t[0] / (double) T);
			double y = start.y + 4 * TAKOMARU_SUPER_JUMP_HEIGHT_BLOCKS * f * (1 - f);
			moveTo(u, start.x + (land.x - start.x) * f, y, start.z + (land.z - start.z) * f);
			lvl.sendParticles(ParticleTypes.CLOUD, u.getX(), u.getY() + 0.2, u.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
			if (t[0] < T) return false;
			// 着地
			moveTo(u, land.x, land.y, land.z);
			double r = Tiles.b(TAKOMARU_SUPER_RADIUS_TILES);
			List<LivingEntity> near = new ArrayList<>();
			for (LivingEntity e : Combat.participants(lvl)) {
				if (e != u && !Combat.sameSide(u, e) && e.position().distanceToSqr(land) <= r * r) near.add(e);
			}
			lvl.sendParticles(ParticleTypes.EXPLOSION, land.x, land.y + 0.3, land.z, 2, 0.6, 0.1, 0.6, 0);
			lvl.sendParticles(ParticleTypes.POOF, land.x, land.y + 0.2, land.z, 20, r * 0.5, 0.1, r * 0.5, 0.1);
			sound(lvl, land, SoundEvents.GENERIC_BIG_FALL, 1.0f, 0.7f);
			sound(lvl, land, SoundEvents.ANVIL_LAND, 0.8f, 0.8f);
			Combat.areaDamage(lvl, u, land, r, TAKOMARU_SUPER_DMG, false);
			for (LivingEntity e : near) {
				BrawlState st = Combat.state(e);
				if (st == null || !st.alive) continue;
				Vec3 away = e.position().subtract(land).multiply(1, 0, 1);
				Vec3 kd = away.lengthSqr() < 1e-4 ? d : away.normalize();
				Combat.knockback(e, kd, Tiles.b(TAKOMARU_SUPER_KNOCK_TILES));
			}
			breakWallsAround(lvl, land, r);
			return true;
		});
	}

	// ---------- ニタ: 相棒のクマ ----------

	/** 狙った方向（最大 NITA_SUPER_RANGE タイル先。壁の中なら手前）にクマを呼ぶ。前の相棒は消える。 */
	private static void summonBear(ServerLevel lvl, LivingEntity u, Vec3 d) {
		Combat.dismissSummons(u.getUUID());
		double dist = Tiles.b(NITA_SUPER_RANGE);
		Vec3 p = u.position().add(d.scale(dist));
		while (dist > 1.0 && blocked(lvl, p)) {
			dist -= 0.5;
			p = u.position().add(d.scale(dist));
		}
		if (dist <= 1.0) p = u.position().add(d.scale(1.0));
		sound(lvl, p, SoundEvents.POLAR_BEAR_WARNING, 1.2f, 0.8f);
		lvl.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.5, p.z, 25, 0.5, 0.6, 0.5, 0.08);
		lvl.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1.0, p.z, 15, 0.5, 0.8, 0.5, 0.3);
		com.amaguri.brawl.bot.BrawlBot.spawnSummon(lvl, p, "nitabear", "クマ", u);
	}

	// ---------- バーリー: ポーション ----------

	/** 向きしか分からないので、正面±12度で射程内の一番近い敵（茂みで隠れていない）の距離に落とす。居なければ最大射程。 */
	private static double lobDistance(ServerLevel lvl, LivingEntity u, Vec3 d) {
		double max = Tiles.b(BARLEY_RANGE), best = max;
		double cos = Math.cos(Math.toRadians(12));
		for (LivingEntity e : Combat.participants(lvl)) {
			if (e == u || Combat.sameSide(u, e) || Combat.isHiddenFrom(e, u)) continue;
			Vec3 to = e.position().subtract(u.position()).multiply(1, 0, 1);
			double len = to.length();
			if (len < 0.5 || len > max) continue;
			if (to.scale(1.0 / len).dot(d) >= cos && len < best) best = len;
		}
		return best;
	}

	/** 放物線でポーションを投げ（壁を越える）、着地点に毒の水たまり。 */
	private static void lobPotion(ServerLevel lvl, LivingEntity u, Vec3 land, boolean main, int puddleTicks) {
		Vec3 start = new Vec3(u.getX(), u.getY() + 1.4, u.getZ());
		Vec3 end = new Vec3(land.x, u.getY(), land.z);
		int[] t = {0};
		Tasks.add(() -> {
			t[0]++;
			double f = Math.min(1.0, t[0] / (double) BARLEY_LOB_TICKS);
			double y = start.y + (end.y - start.y) * f + 4 * 3.0 * f * (1 - f);
			lvl.sendParticles(POTION_DUST, start.x + (end.x - start.x) * f, y, start.z + (end.z - start.z) * f, 2, 0.05, 0.05, 0.05, 0);
			if (t[0] < BARLEY_LOB_TICKS) return false;
			sound(lvl, end, SoundEvents.SPLASH_POTION_BREAK, 0.9f, 1.0f);
			lvl.sendParticles(ParticleTypes.SPLASH, end.x, end.y + 0.3, end.z, 20, 0.6, 0.2, 0.6, 0.1);
			puddle(lvl, u, end, puddleTicks, main);
			return true;
		});
	}

	/** 毒の水たまり。緑の泡のパーティクルを出し、10tickごとに DPS/2 の範囲ダメージ。 */
	private static void puddle(ServerLevel lvl, LivingEntity owner, Vec3 c, int ticks, boolean main) {
		double r = Tiles.b(BARLEY_PUDDLE_RADIUS_TILES);
		int dmg = BARLEY_DPS * BARLEY_TICK_INTERVAL / 20;
		int[] t = {0};
		Tasks.add(() -> {
			if (t[0] % 2 == 0) {
				lvl.sendParticles(POISON_DUST, c.x, c.y + 0.15, c.z, 6, r * 0.5, 0.05, r * 0.5, 0);
				lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 0.3, c.z, 2, r * 0.5, 0.1, r * 0.5, 0);
				lvl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 0.2, c.z, 3, r * 0.5, 0.1, r * 0.5, 0.01);
			}
			if (t[0] % BARLEY_TICK_INTERVAL == 0 && t[0] > 0) {
				// 最初の1回だけゲージが溜まる（毎回溜めると水たまりだけで満タンになる）
				Combat.areaDamage(lvl, owner, c, r, dmg, main && t[0] == BARLEY_TICK_INTERVAL);
			}
			return ++t[0] > ticks;
		});
	}

	/** 必殺: 前方の広い範囲に複数のポーション。 */
	private static void potionRain(ServerLevel lvl, LivingEntity u, Vec3 d) {
		Vec3 center = u.position().add(d.scale(Tiles.b(BARLEY_SUPER_CENTER_TILES)));
		double area = Tiles.b(BARLEY_SUPER_AREA_TILES);
		sound(lvl, u.position(), SoundEvents.WITCH_THROW, 1.0f, 0.7f);
		for (int i = 0; i < BARLEY_SUPER_BOTTLES; i++) {
			double ang = lvl.getRandom().nextDouble() * Math.PI * 2;
			double rad = Math.sqrt(lvl.getRandom().nextDouble()) * area;
			Vec3 land = new Vec3(center.x + Math.cos(ang) * rad, center.y, center.z + Math.sin(ang) * rad);
			Tasks.after(i * BARLEY_SUPER_INTERVAL_TICKS, () -> {
				if (u.isRemoved()) return;
				lobPotion(lvl, u, land, false, BARLEY_SUPER_PUDDLE_TICKS);
			});
		}
	}

	/** 足元か頭が弾を止めるブロックなら true。 */
	private static boolean blocked(ServerLevel lvl, Vec3 p) {
		BlockPos b = BlockPos.containing(p.x, p.y + 0.1, p.z);
		return Arena.isBulletSolid(lvl.getBlockState(b)) || Arena.isBulletSolid(lvl.getBlockState(b.above()));
	}
}
