package com.amaguri.brawl.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 氷の床（lou の必殺）。範囲と残り時間を持ち、その上の敵の「進む向きの変化」を見て
 * 減速（向きを変えた）／一時停止（急に変えた）にする。見た目は白〜水色のパーティクル（専用描画は後で）。
 */
final class IceFloors {
	private IceFloors() {}

	private static final ParticleOptions ICE_WHITE = new DustParticleOptions(0xF2FBFF, 1.1f);
	private static final ParticleOptions ICE_BLUE = new DustParticleOptions(0x9FDCFF, 1.3f);

	/** 1tickの移動がこれ未満なら「止まっている」として向きを見ない（ブロック/tick）。 */
	private static final double MIN_MOVE = 0.04;
	/** 向きの変化 約32度以上で減速、約100度以上で一時停止。 */
	private static final double COS_SLOW = 0.85, COS_STOP = -0.17;
	private static final int RETRIGGER_TICKS = 10;

	private static final class Track {
		Vec3 last;
		Vec3 ref; // 進行方向の基準（ゆっくり追従するので、なだらかなカーブでは反応しない）
		long cooldownUntil;
	}

	private static final class Floor {
		final ServerLevel lvl;
		final LivingEntity owner;
		final Vec3 center;
		final double radius;
		final long endTick;
		final int slowTicks, stopTicks;
		final double slowMul;
		final Map<UUID, Track> tracks = new HashMap<>();

		Floor(ServerLevel lvl, LivingEntity owner, Vec3 center, double radius, long endTick, int slowTicks, double slowMul, int stopTicks) {
			this.lvl = lvl;
			this.owner = owner;
			this.center = center;
			this.radius = radius;
			this.endTick = endTick;
			this.slowTicks = slowTicks;
			this.slowMul = slowMul;
			this.stopTicks = stopTicks;
		}
	}

	private static final List<Floor> FLOORS = new ArrayList<>();

	static void add(ServerLevel lvl, LivingEntity owner, Vec3 center, double radius, int ticks, int slowTicks, double slowMul, int stopTicks) {
		FLOORS.add(new Floor(lvl, owner, center, radius, Combat.now() + ticks, slowTicks, slowMul, stopTicks));
	}

	static void clear() {
		FLOORS.clear();
	}

	static void tick(long now) {
		if (FLOORS.isEmpty()) return;
		FLOORS.removeIf(f -> {
			try {
				return tickOne(f, now);
			} catch (RuntimeException ex) {
				return true;
			}
		});
	}

	/** true で終了。 */
	private static boolean tickOne(Floor f, long now) {
		if (now >= f.endTick) return true;
		ServerLevel lvl = f.lvl;
		Vec3 c = f.center;
		double r = f.radius;

		// 見た目（2tickに1回）
		if ((now & 1) == 0) {
			int n = Math.max(6, (int) (r * r * 0.9));
			lvl.sendParticles(ICE_WHITE, c.x, c.y + 0.12, c.z, n, r * 0.6, 0.03, r * 0.6, 0);
			lvl.sendParticles(ICE_BLUE, c.x, c.y + 0.15, c.z, n / 2, r * 0.6, 0.03, r * 0.6, 0);
			lvl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 0.3, c.z, 3, r * 0.5, 0.2, r * 0.5, 0.01);
			// 縁
			if ((now & 3) == 0) {
				for (int i = 0; i < 12; i++) {
					double a = (Math.PI * 2) * (i + (now % 8) / 8.0) / 12.0;
					lvl.sendParticles(ICE_BLUE, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
			}
		}

		// 敵の動き
		List<UUID> inside = new ArrayList<>();
		for (LivingEntity e : Combat.participants(lvl)) {
			if (e == f.owner || (f.owner != null && Combat.sameSide(f.owner, e))) continue;
			BrawlState st = Combat.state(e);
			if (st == null || !st.alive) continue;
			double dx = e.getX() - c.x, dz = e.getZ() - c.z;
			if (dx * dx + dz * dz > r * r) continue;
			inside.add(e.getUUID());
			st.onIceUntil = now + 3;
			Track t = f.tracks.computeIfAbsent(e.getUUID(), k -> new Track());
			Vec3 pos = e.position();
			Vec3 prev = t.last;
			t.last = pos;
			if (prev == null) continue;
			double mx = pos.x - prev.x, mz = pos.z - prev.z;
			double len = Math.sqrt(mx * mx + mz * mz);
			if (st.pushUntil > now) { // 突進・押し出しは「向きを変えた」に数えない
				t.ref = null;
				continue;
			}
			if (len < MIN_MOVE) continue;
			Vec3 dir = new Vec3(mx / len, 0, mz / len);
			if (t.ref == null) {
				t.ref = dir;
				continue;
			}
			double cos = dir.x * t.ref.x + dir.z * t.ref.z;
			if (now >= t.cooldownUntil && cos < COS_SLOW) {
				t.cooldownUntil = now + RETRIGGER_TICKS;
				Combat.slow(e, f.slowTicks, f.slowMul);
				if (cos < COS_STOP) Combat.stun(e, f.stopTicks);
				lvl.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.2, pos.z, 8, 0.3, 0.1, 0.3, 0.05);
				t.ref = dir;
			} else {
				double rx = t.ref.x * 0.65 + dir.x * 0.35, rz = t.ref.z * 0.65 + dir.z * 0.35;
				double rl = Math.sqrt(rx * rx + rz * rz);
				t.ref = rl < 1e-6 ? dir : new Vec3(rx / rl, 0, rz / rl);
			}
		}
		f.tracks.keySet().retainAll(inside);
		return false;
	}
}
