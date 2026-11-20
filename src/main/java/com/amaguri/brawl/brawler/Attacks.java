package com.amaguri.brawl.brawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
	private static final ParticleOptions SNOW_DUST = new DustParticleOptions(0xE8F6FF, 1.0f);
	private static final ParticleOptions BAT_DUST = new DustParticleOptions(0x4A3560, 1.1f);

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

	static void normal(ServerLevel lvl, LivingEntity u, String id, float yaw, float aimDist) {
		Vec3 d = dir(yaw);
		Vec3 m = muzzle(u, d);
		switch (id) {
			case "rusty" -> {
				muzzleFlash(lvl, m, d, ParticleTypes.FLAME);
				sound(lvl, m, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.2f);
				fan(lvl, u, yaw, RUSTY_PELLETS, RUSTY_FAN, RUSTY_SPEED_BPT, RUSTY_RANGE, RUSTY_PELLET_RADIUS,
						RUSTY_DMG, true, false, ORANGE_SPARK, null);
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
			case "frank" -> frankSlam(lvl, u, yaw, false);
			case "darryl" -> {
				sound(lvl, m, SoundEvents.COPPER_GOLEM_HURT, 0.9f, 0.7f);
				for (int v = 0; v < DARRYL_VOLLEYS; v++) {
					Tasks.after(v * DARRYL_VOLLEY_INTERVAL_TICKS, () -> {
						if (!active(u) || !Combat.canAct(u)) return;
						Vec3 mm = muzzle(u, d);
						muzzleFlash(lvl, mm, d, ParticleTypes.FLAME);
						sound(lvl, mm, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 0.9f);
						fan(lvl, u, yaw, DARRYL_PELLETS, DARRYL_FAN, DARRYL_SPEED_BPT, DARRYL_RANGE, DARRYL_PELLET_RADIUS,
								DARRYL_DMG, true, false, ORANGE_SPARK, null);
					});
				}
			}
			case "poco" -> {
				sound(lvl, m, SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.2f);
				sharedFan(lvl, u, yaw, 9, POCO_FAN, POCO_WAVE_SPEED_BPT, POCO_RANGE, POCO_WAVE_RADIUS, POCO_DMG, true, false,
						ParticleTypes.NOTE, null);
			}
			case "mortis" -> {
				sound(lvl, m, SoundEvents.BAT_TAKEOFF, 1.0f, 1.2f);
				dash(lvl, u, d, MORTIS_DASH_TILES, MORTIS_DASH_TICKS, MORTIS_DMG, 0, MORTIS_DASH_HIT_RADIUS_BLOCKS, false, true, null);
			}
			case "edgar" -> {
				for (int i = 0; i < EDGAR_PUNCHES; i++) {
					Tasks.after(i * EDGAR_PUNCH_INTERVAL_TICKS, () -> {
						if (!active(u) || !Combat.canAct(u)) return;
						Vec3 mm = muzzle(u, d);
						Vec3 f = mm.add(d.scale(1.0));
						lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, f.x, f.y, f.z, 1, 0, 0, 0, 0);
						sound(lvl, mm, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
						shoot(lvl, u, yaw, EDGAR_PUNCH_SPEED_BPT, EDGAR_RANGE, EDGAR_PUNCH_RADIUS, EDGAR_DMG, true, false,
								ParticleTypes.CRIT, null, null);
					});
				}
			}
			case "lou" -> {
				for (int i = 0; i < LOU_BALLS; i++) {
					Tasks.after(i * LOU_BALL_INTERVAL_TICKS, () -> {
						if (!active(u) || !Combat.canAct(u)) return;
						Vec3 mm = muzzle(u, d);
						sound(lvl, mm, SoundEvents.SNOW_GOLEM_SHOOT, 0.8f, 1.1f);
						shoot(lvl, u, yaw, LOU_BALL_SPEED_BPT, LOU_RANGE, LOU_BALL_RADIUS, LOU_DMG, true, false, SNOW_DUST,
								(l, sh, t, p) -> {
									Combat.damage(t, u, LOU_DMG, true);
									Combat.addFrost(t, LOU_FROST_PER_HIT);
									l.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 8, 0.25, 0.25, 0.25, 0.04);
								}, null);
					});
				}
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
				double ld = aimDist > 0 ? Math.min(aimDist, Tiles.b(BARLEY_RANGE)) : lobDistance(lvl, u, d);
				Vec3 land = u.position().add(d.scale(ld));
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

	static void superMove(ServerLevel lvl, LivingEntity u, String id, float yaw, float aimDist) {
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
			case "gouen" -> {
				sound(lvl, u.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.6f);
				dash(lvl, u, d, GOUEN_SUPER_DASH_TILES, GOUEN_SUPER_DASH_TICKS, GOUEN_SUPER_DMG, GOUEN_SUPER_KNOCK_TILES,
						GOUEN_SUPER_HIT_RADIUS_BLOCKS, true, false, null);
			}
			case "frank" -> frankSlam(lvl, u, yaw, true);
			case "darryl" -> {
				sound(lvl, u.position(), SoundEvents.BARREL_OPEN, 1.0f, 0.6f);
				dash(lvl, u, d, DARRYL_SUPER_DASH_TILES, DARRYL_SUPER_DASH_TICKS, DARRYL_SUPER_DMG, DARRYL_SUPER_KNOCK_TILES,
						DARRYL_SUPER_HIT_RADIUS_BLOCKS, false, false, null);
			}
			case "poco" -> pocoHeal(lvl, u, yaw);
			case "mortis" -> mortisBats(lvl, u, yaw);
			case "edgar" -> edgarLeap(lvl, u, d, aimDist);
			case "lou" -> iceFloor(lvl, u, d, aimDist);
			case "mimosa" -> brockRain(lvl, u, d, aimDist);
			case "nita" -> summonBear(lvl, u, d, aimDist);
			case "barley" -> potionRain(lvl, u, d, aimDist);
			default -> {}
		}
	}

	/**
	 * 突進（共通）。tiles タイルを ticks tick で進み、触れた敵に dmg と押し出し（knockTiles>0 のとき）。
	 * breaks=true なら進行方向の壁を壊し、false なら壁で止まる。スタン・凍結・脱落で中断。
	 */
	private static void dash(ServerLevel lvl, LivingEntity u, Vec3 d, double tiles, int ticks, int dmg, double knockTiles,
			double hitRadiusBlocks, boolean breaks, boolean main, Consumer<LivingEntity> onHit) {
		double perTick = Tiles.b(tiles) / ticks;
		Set<LivingEntity> hit = new HashSet<>();
		boolean[] hitBox = {false};
		int[] t = {0};
		Tasks.add(() -> {
			if (!active(u) || Combat.disabled(u) || t[0]++ >= ticks) return true;
			Vec3 p = u.position();
			// 突進の先にパワーキューブ箱があれば1回ダメージ（モーティス・ダリル・ウシの突進でも箱を壊せるように）
			if (!hitBox[0]) {
				Vec3 q = p.add(d.scale(perTick + 0.8));
				for (double dy : new double[] {0.5, 1.5}) {
					BlockPos bp = BlockPos.containing(q.x, p.y + dy, q.z);
					if (lvl.getBlockState(bp).getBlock() == com.amaguri.brawl.arena.ArenaBlocks.BOX) {
						com.amaguri.brawl.core.BrawlState os = Combat.state(u);
						com.amaguri.brawl.mode.Boxes.damage(lvl, bp, Math.round(dmg * (os == null ? 1f : os.damageMul())), u);
						hitBox[0] = true;
						break;
					}
				}
			}
			if (!breaks && blocked(lvl, p.add(d.scale(perTick + 0.6)))) return true; // 壁で止まる
			Combat.markPush(u, 3);
			u.setDeltaMovement(d.x * perTick, u.getDeltaMovement().y, d.z * perTick);
			u.hurtMarked = true; // プレイヤーにも速度を同期
			lvl.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.2, p.z, 3, 0.3, 0.1, 0.3, 0.02);
			lvl.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1.0, p.z, 2, 0.3, 0.3, 0.3, 0.1);
			if (breaks) {
				// 進行方向の壁を壊す（弾ではないので自前で）
				for (double k = 1.5; k <= 3.0; k += 1.5) {
					Vec3 q = p.add(d.scale(k));
					Arena.breakTile(lvl, BlockPos.containing(q.x, q.y + 0.5, q.z));
					Arena.breakTile(lvl, BlockPos.containing(q.x, q.y + 1.5, q.z));
				}
			}
			double r2 = hitRadiusBlocks * hitRadiusBlocks;
			for (LivingEntity e : new ArrayList<>(Combat.participants(lvl))) {
				if (e == u || Combat.sameSide(u, e) || hit.contains(e) || e.position().distanceToSqr(p) > r2) continue;
				hit.add(e);
				Combat.damage(e, u, dmg, main);
				if (knockTiles > 0) Combat.knockback(e, d, Tiles.b(knockTiles));
				if (onHit != null) onHit.accept(e);
				sound(lvl, e.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
			}
			return false;
		});
	}

	/**
	 * 扇状に n 発の貫通弾を撃つが、同じ敵には1回しか当てない（1発の広い衝撃波として扱う）。
	 * extra は命中した敵ごとに（ダメージの後に）呼ぶ。null 可。
	 */
	private static void sharedFan(ServerLevel lvl, LivingEntity u, float yaw, int n, double fanDeg, double speed, double rangeTiles,
			double radius, int dmg, boolean main, boolean breaks, ParticleOptions trail, Consumer<LivingEntity> extra) {
		Set<UUID> seen = new HashSet<>();
		double step = n > 1 ? fanDeg / (n - 1) : 0;
		for (int i = 0; i < n; i++) {
			double off = n > 1 ? -fanDeg / 2 + step * i : 0;
			Vec3 d = dir(yaw + off);
			Projectiles.spawn(lvl, new Shot(u, muzzle(u, d), d, speed, Tiles.b(rangeTiles), radius, dmg, main, breaks, true, trail,
					(l, sh, t, p) -> {
						if (!seen.add(t.getUUID())) return;
						Combat.damage(t, u, dmg, main);
						if (extra != null) extra.accept(t);
					}, null));
		}
	}

	// ---------- フランク: ハンマー（溜め1.3秒・動けない）／必殺は即時の衝撃波でスタン ----------

	private static void frankSlam(ServerLevel lvl, LivingEntity u, float yaw, boolean isSuper) {
		if (isSuper) {
			Vec3 m = muzzle(u, dir(yaw));
			sound(lvl, m, SoundEvents.ANVIL_LAND, 1.0f, 0.6f);
			lvl.sendParticles(ParticleTypes.EXPLOSION, m.x + dir(yaw).x * 2, m.y - 0.5, m.z + dir(yaw).z * 2, 1, 0, 0, 0, 0);
			sharedFan(lvl, u, yaw, 13, FRANK_FAN + 10, FRANK_BULLET_SPEED_BPT, FRANK_SUPER_RANGE, FRANK_BULLET_RADIUS, FRANK_SUPER_DMG,
					false, true, ParticleTypes.CRIT, t -> Combat.stun(t, FRANK_SUPER_STUN_TICKS));
			return;
		}
		int token = Combat.beginCharge(u, FRANK_CHARGE_TICKS);
		sound(lvl, u.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.5f);
		int[] t = {0};
		Tasks.add(() -> {
			if (!Combat.chargeValid(u, token)) return true; // スタンなどで中断
			Vec3 p = u.position();
			if (t[0] % 4 == 0) lvl.sendParticles(ParticleTypes.CRIT, p.x, p.y + 2.2, p.z, 3, 0.3, 0.2, 0.3, 0.1);
			if (++t[0] < FRANK_CHARGE_TICKS) return false;
			Combat.endCharge(u);
			Vec3 d = dir(yaw);
			Vec3 m = muzzle(u, d);
			sound(lvl, m, SoundEvents.ANVIL_LAND, 0.9f, 0.9f);
			lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, m.x + d.x * 2, m.y - 0.3, m.z + d.z * 2, 1, 0, 0, 0, 0);
			sharedFan(lvl, u, yaw, 11, FRANK_FAN, FRANK_BULLET_SPEED_BPT, FRANK_RANGE, FRANK_BULLET_RADIUS, FRANK_DMG, true, false,
					ParticleTypes.CRIT, null);
			return true;
		});
	}

	// ---------- ポコ: 回復の扇（ソロでは自分と自分の召喚物） ----------

	private static void pocoHeal(ServerLevel lvl, LivingEntity u, float yaw) {
		Vec3 p = u.position();
		Combat.heal(u, POCO_SUPER_HEAL);
		for (LivingEntity e : Combat.participants(lvl)) {
			// 味方の概念があるのは召喚物（ニタのクマ）だけ。近くにいれば回復
			if (e != u && Combat.sameSide(u, e) && e.position().distanceToSqr(p) <= Tiles.b(POCO_SUPER_RANGE) * Tiles.b(POCO_SUPER_RANGE)) {
				Combat.heal(e, POCO_SUPER_HEAL);
				lvl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 1.8, e.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
			}
		}
		lvl.sendParticles(ParticleTypes.HEART, u.getX(), u.getY() + 2.0, u.getZ(), 8, 0.5, 0.3, 0.5, 0.0);
		sound(lvl, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
		sound(lvl, p, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.4f);
		sharedFan(lvl, u, yaw, 13, POCO_SUPER_FAN, POCO_WAVE_SPEED_BPT, POCO_SUPER_RANGE, POCO_WAVE_RADIUS, POCO_SUPER_DMG, false, false,
				ParticleTypes.NOTE, null);
	}

	// ---------- モーティス: コウモリの群れ（当たった敵ごとに回復） ----------

	private static void mortisBats(ServerLevel lvl, LivingEntity u, float yaw) {
		Vec3 m = muzzle(u, dir(yaw));
		sound(lvl, m, SoundEvents.BAT_TAKEOFF, 1.0f, 0.8f);
		sound(lvl, m, SoundEvents.BAT_AMBIENT, 0.8f, 0.9f);
		sharedFan(lvl, u, yaw, 9, MORTIS_SUPER_FAN, MORTIS_BAT_SPEED_BPT, MORTIS_SUPER_RANGE, MORTIS_BAT_RADIUS, MORTIS_SUPER_DMG, false, false,
				BAT_DUST, t -> {
					Combat.heal(u, MORTIS_SUPER_HEAL_PER_HIT);
					lvl.sendParticles(ParticleTypes.HEART, u.getX(), u.getY() + 1.8, u.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
				});
	}

	// ---------- エドガー: 跳躍（壁を越える）→ 着地後に加速 ----------

	private static void edgarLeap(ServerLevel lvl, LivingEntity u, Vec3 d, float aimDist) {
		// 向きしか分からないので、正面の敵がいればその手前に降りる（lobDistance と同じ考え方）
		double max = Tiles.b(EDGAR_SUPER_LEAP_TILES);
		double dist = aimDist > 0 ? Math.min(max, Math.max(1.0, aimDist))
				: Math.min(max, Math.max(1.0, aimDistance(lvl, u, d, max) - 1.2));
		leap(lvl, u, d, dist, EDGAR_SUPER_FLIGHT_TICKS, EDGAR_SUPER_HEIGHT_BLOCKS, land -> {
			Combat.haste(u, EDGAR_HASTE_TICKS, Tiles.b(EDGAR_HASTE_TILES_PER_SEC));
			lvl.sendParticles(ParticleTypes.POOF, land.x, land.y + 0.2, land.z, 12, 0.5, 0.1, 0.5, 0.08);
			sound(lvl, land, SoundEvents.WITHER_SKELETON_HURT, 0.7f, 1.4f);
		});
	}

	/** 放物線で跳ぶ（壁・敵を越える）。着地点が壁の中なら手前に戻す。スタン・脱落で中断（その場に落ちる）。 */
	private static void leap(ServerLevel lvl, LivingEntity u, Vec3 d, double distBlocks, int flightTicks, double heightBlocks,
			Consumer<Vec3> onLand) {
		Vec3 start = u.position();
		double dist = distBlocks;
		Vec3 end = start.add(d.scale(dist));
		while (dist > 0 && blocked(lvl, end)) {
			dist -= 0.5;
			end = start.add(d.scale(dist));
		}
		if (dist <= 0) end = start;
		final Vec3 land = end;
		int[] t = {0};
		sound(lvl, start, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.6f);
		Tasks.add(() -> {
			if (!active(u) || Combat.disabled(u)) return true;
			t[0]++;
			double f = Math.min(1.0, t[0] / (double) flightTicks);
			double y = start.y + 4 * heightBlocks * f * (1 - f);
			Combat.markPush(u, 3);
			moveTo(u, start.x + (land.x - start.x) * f, y, start.z + (land.z - start.z) * f);
			lvl.sendParticles(ParticleTypes.CLOUD, u.getX(), u.getY() + 0.2, u.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
			if (t[0] < flightTicks) return false;
			moveTo(u, land.x, land.y, land.z);
			onLand.accept(land);
			return true;
		});
	}

	/** 正面±12度で max ブロック以内の一番近い敵（茂みで隠れていない）までの距離。居なければ max。 */
	private static double aimDistance(ServerLevel lvl, LivingEntity u, Vec3 d, double max) {
		double best = max;
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

	// ---------- ルー: 氷の床 ----------

	private static void iceFloor(ServerLevel lvl, LivingEntity u, Vec3 d, float aimDist) {
		// 向きしか分からないので、正面の敵がいればその位置（最大8タイル）に敷く
		double max = Tiles.b(LOU_SUPER_RANGE);
		double dist = aimDist > 0 ? Math.min(max, Math.max(1.5, aimDist)) : Math.max(1.5, aimDistance(lvl, u, d, max));
		Vec3 p = u.position().add(d.scale(dist));
		while (dist > 1.0 && blocked(lvl, p)) {
			dist -= 0.5;
			p = u.position().add(d.scale(dist));
		}
		if (dist <= 1.0) p = u.position().add(d.scale(1.0));
		sound(lvl, p, SoundEvents.GLASS_BREAK, 1.0f, 1.5f);
		lvl.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.5, p.z, 25, 1.0, 0.4, 1.0, 0.05);
		Combat.addIceFloor(lvl, u, p, Tiles.b(LOU_FLOOR_RADIUS_TILES), LOU_FLOOR_TICKS, LOU_SLOW_TICKS, LOU_SLOW_MUL, LOU_STOP_TICKS);
	}

	private static void brockRain(ServerLevel lvl, LivingEntity u, Vec3 d, float aimDist) {
		double cd = aimDist > 0 ? Math.min(aimDist, Tiles.b(MIMOSA_SUPER_CENTER_TILES)) : Tiles.b(MIMOSA_SUPER_CENTER_TILES);
		Vec3 center = u.position().add(d.scale(cd));
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

	// ---------- ニタ: 相棒のクマ ----------

	/** 狙った方向（最大 NITA_SUPER_RANGE タイル先。壁の中なら手前）にクマを呼ぶ。前の相棒は消える。 */
	private static void summonBear(ServerLevel lvl, LivingEntity u, Vec3 d, float aimDist) {
		Combat.dismissSummons(u.getUUID());
		double dist = aimDist > 0 ? Math.min(aimDist, Tiles.b(NITA_SUPER_RANGE)) : Tiles.b(NITA_SUPER_RANGE);
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
		// 本家の弾速 1750（=0.583ブロック/tick）で飛ぶ時間
		int flight = Math.max(6, (int) Math.round(Math.hypot(end.x - start.x, end.z - start.z) / BARLEY_LOB_SPEED_BPT));
		Tasks.add(() -> {
			t[0]++;
			double f = Math.min(1.0, t[0] / (double) flight);
			double y = start.y + (end.y - start.y) * f + 4 * 3.0 * f * (1 - f);
			lvl.sendParticles(POTION_DUST, start.x + (end.x - start.x) * f, y, start.z + (end.z - start.z) * f, 2, 0.05, 0.05, 0.05, 0);
			if (t[0] < flight) return false;
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
	private static void potionRain(ServerLevel lvl, LivingEntity u, Vec3 d, float aimDist) {
		double cd = aimDist > 0 ? Math.min(aimDist, Tiles.b(BARLEY_SUPER_CENTER_TILES)) : Tiles.b(BARLEY_SUPER_CENTER_TILES);
		Vec3 center = u.position().add(d.scale(cd));
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
