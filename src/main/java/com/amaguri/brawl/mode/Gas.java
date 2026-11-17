package com.amaguri.brawl.mode;

import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.api.Tiles;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 毒ガス。開始40秒後から120秒かけて、中央の8x8タイル（16x16ブロック）まで縮む。 */
public final class Gas {
	private Gas() {}

	static final int START_TICKS = 40 * 20;
	static final int SHRINK_TICKS = 120 * 20;
	private static final double FINAL_HALF = Tiles.b(8) / 2.0; // 8タイル四方 → 半径4タイル = 8ブロック

	private static boolean active;
	private static double fMinX, fMinZ, fMaxX, fMaxZ, cx, cz, minY, maxY;
	private static AABB zone;

	/** 試合開始時（アリーナが決まったとき）に呼ぶ。 */
	static void reset(ArenaLayout l) {
		fMinX = l.origin().getX();
		fMinZ = l.origin().getZ();
		fMaxX = fMinX + Tiles.b(l.widthTiles());
		fMaxZ = fMinZ + Tiles.b(l.depthTiles());
		cx = l.center().x;
		cz = l.center().z;
		minY = l.origin().getY() - 64;
		maxY = l.origin().getY() + 256;
		zone = new AABB(fMinX, minY, fMinZ, fMaxX, maxY, fMaxZ);
		active = true;
	}

	static void clear() {
		active = false;
		zone = null;
	}

	/** 経過tick（試合開始=0）に合わせて安全地帯を更新。 */
	static void update(int playTicks) {
		if (!active) return;
		double f = Math.max(0.0, Math.min(1.0, (playTicks - START_TICKS) / (double) SHRINK_TICKS));
		double minX = lerp(fMinX, cx - FINAL_HALF, f);
		double maxX = lerp(fMaxX, cx + FINAL_HALF, f);
		double mnZ = lerp(fMinZ, cz - FINAL_HALF, f);
		double mxZ = lerp(fMaxZ, cz + FINAL_HALF, f);
		zone = new AABB(minX, minY, mnZ, maxX, maxY, mxZ);
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/** 次に縮み始めるまでの秒。縮み中・縮み終わりは0。 */
	static int secondsToNext(int playTicks) {
		if (playTicks >= START_TICKS) return 0;
		return (START_TICKS - playTicks + 19) / 20;
	}

	/** 縮みが始まっているか（縁を描く条件）。 */
	static boolean shrinking(int playTicks) {
		return active && playTicks >= START_TICKS;
	}

	public static boolean inGas(Vec3 pos) {
		if (!active || zone == null) return false;
		return pos.x < zone.minX || pos.x > zone.maxX || pos.z < zone.minZ || pos.z > zone.maxZ;
	}

	public static AABB safeZone() {
		if (zone != null) return zone;
		return new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000); // アリーナ不明のとき（全域）
	}

	/** 縁の緑パーティクル。受け手の近く（24ブロック以内の辺）だけ出す。 */
	static void edgeParticles(ServerLevel lvl, ServerPlayer p) {
		if (zone == null) return;
		DustParticleOptions opt = new DustParticleOptions(0x33FF55, 1.6f);
		double px = p.getX(), py = p.getY(), pz = p.getZ();
		final double near = 24, span = 16, step = 2;
		if (Math.abs(px - zone.minX) < near) strip(lvl, p, opt, zone.minX, true, pz, zone.minZ, zone.maxZ, py, span, step);
		if (Math.abs(px - zone.maxX) < near) strip(lvl, p, opt, zone.maxX, true, pz, zone.minZ, zone.maxZ, py, span, step);
		if (Math.abs(pz - zone.minZ) < near) strip(lvl, p, opt, zone.minZ, false, px, zone.minX, zone.maxX, py, span, step);
		if (Math.abs(pz - zone.maxZ) < near) strip(lvl, p, opt, zone.maxZ, false, px, zone.minX, zone.maxX, py, span, step);
	}

	private static void strip(ServerLevel lvl, ServerPlayer p, DustParticleOptions opt, double fixed, boolean fixedIsX,
			double along, double lo, double hi, double py, double span, double step) {
		double from = Math.max(lo, along - span);
		double to = Math.min(hi, along + span);
		for (double a = from; a <= to; a += step) {
			for (double h = -1; h <= 4; h += 1.5) {
				double x = fixedIsX ? fixed : a;
				double z = fixedIsX ? a : fixed;
				lvl.sendParticles(p, opt, false, true, x, py + h, z, 1, 0, 0, 0, 0);
			}
		}
	}
}
