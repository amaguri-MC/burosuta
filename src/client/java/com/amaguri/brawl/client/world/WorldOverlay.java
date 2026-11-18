package com.amaguri.brawl.client.world;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.api.Tiles;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.client.hud.HudState;
import com.amaguri.brawl.client.view.TopDown;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/** G: 毒ガスの壁・外側の塗りと、射程ガイドをワールド内に直接描く（パーティクルは遅れるので使わない）。 */
public final class WorldOverlay {
	private WorldOverlay() {}

	private static final int GLFW_KEY_R = 82;
	private static final double WALL_H = 6.0;

	// 毒ガス: 初期の四角（アリーナ全体）と、補間した現在の四角
	private static boolean haveInit = false;
	private static double iMinX, iMinZ, iMaxX, iMaxZ;
	private static double cMinX, cMinZ, cMaxX, cMaxZ;
	private static double floorY = Double.NaN;
	private static long lastNanos = 0;

	public static void init() {
		// UNVERIFIED: COLLECT_SUBMITS で submitCustomGeometry した半透明ジオメトリが同フレームの半透明の後に描かれること
		LevelRenderEvents.COLLECT_SUBMITS.register(WorldOverlay::submit);
	}

	private static void submit(LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) { reset(); return; }
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 cam = ctx.levelState().cameraRenderState.pos;
		PoseStack ps = ctx.poseStack();

		Net.MatchPayload m = HudState.match;
		if (m == null || m.phase() == 0) {
			reset();
		} else {
			updateGas(m, mc);
			if (!Double.isNaN(floorY)) {
				final double fy = floorY;
				ps.pushPose();
				ctx.submitNodeCollector().submitCustomGeometry(ps, RenderTypes.debugQuads(),
						(pose, vc) -> drawGas(pose, vc, cam, fy));
				ps.popPose();
			}
		}

		if (TopDown.active()) {
			String id = null;
			if (HudState.hud != null && !HudState.hud.brawler().isEmpty()) id = HudState.hud.brawler();
			else if (HudState.lobby != null) id = HudState.lobby.selected();
			BrawlerDef def = Brawlers.get(id);
			if (def != null) {
				boolean sup = InputConstants.isKeyDown(mc.getWindow(), GLFW_KEY_R); // UNVERIFIED: 既定キーRのみ（再割り当ては追従しない）
				double range = (sup ? def.superRangeTiles() : def.attackRangeTiles()) * Tiles.BLOCKS_PER_TILE;
				Vec3 feet = mc.player.getPosition(partial);
				float yaw = TopDown.aimYaw();
				ps.pushPose();
				ctx.submitNodeCollector().submitCustomGeometry(ps, RenderTypes.debugQuads(),
						(pose, vc) -> drawGuide(pose, vc, cam, feet, yaw, range, sup));
				ps.popPose();
			}
		}
	}

	private static void reset() {
		haveInit = false; floorY = Double.NaN; lastNanos = 0;
	}

	private static void updateGas(Net.MatchPayload m, Minecraft mc) {
		long now = System.nanoTime();
		double dt = lastNanos == 0 ? 0.016 : Math.min(0.25, (now - lastNanos) / 1.0e9);
		lastNanos = now;
		if (!haveInit) { // 最初に受け取った四角 = アリーナ全体
			iMinX = cMinX = m.gasMinX(); iMinZ = cMinZ = m.gasMinZ();
			iMaxX = cMaxX = m.gasMaxX(); iMaxZ = cMaxZ = m.gasMaxZ();
			haveInit = true;
		} else {
			double k = 1 - Math.exp(-dt * 6.0);
			cMinX += (m.gasMinX() - cMinX) * k; cMinZ += (m.gasMinZ() - cMinZ) * k;
			cMaxX += (m.gasMaxX() - cMaxX) * k; cMaxZ += (m.gasMaxZ() - cMaxZ) * k;
		}
		// 床の高さ: 接地しているときの足元の高さを覚える
		if (mc.player.onGround() || Double.isNaN(floorY)) floorY = mc.player.getY();
	}

	// ---- 毒ガス ----

	private static void drawGas(PoseStack.Pose pose, VertexConsumer vc, Vec3 cam, double fy) {
		final int fill = argb(0.30f, 0.15f, 0.85f, 0.25f);
		double y = fy + 0.03;
		// 外側の塗り（初期の四角の中で、現在の四角の外）
		hQuad(pose, vc, cam, iMinX, iMinZ, cMinX, iMaxZ, y, fill);       // 西
		hQuad(pose, vc, cam, cMaxX, iMinZ, iMaxX, iMaxZ, y, fill);       // 東
		hQuad(pose, vc, cam, cMinX, iMinZ, cMaxX, cMinZ, y, fill);       // 北
		hQuad(pose, vc, cam, cMinX, cMaxZ, cMaxX, iMaxZ, y, fill);       // 南
		// 壁（下 0.45 → 上 0）
		int lo = argb(0.45f, 0.20f, 0.95f, 0.30f), hi = argb(0.0f, 0.20f, 0.95f, 0.30f);
		vQuad(pose, vc, cam, cMinX, cMinZ, cMaxX, cMinZ, fy, lo, hi); // 北
		vQuad(pose, vc, cam, cMinX, cMaxZ, cMaxX, cMaxZ, fy, lo, hi); // 南
		vQuad(pose, vc, cam, cMinX, cMinZ, cMinX, cMaxZ, fy, lo, hi); // 西
		vQuad(pose, vc, cam, cMaxX, cMinZ, cMaxX, cMaxZ, fy, lo, hi); // 東
	}

	private static void hQuad(PoseStack.Pose p, VertexConsumer vc, Vec3 cam, double x0, double z0, double x1, double z1, double y, int c) {
		if (x1 - x0 < 1e-3 || z1 - z0 < 1e-3) return;
		v(p, vc, cam, x0, y, z0, c); v(p, vc, cam, x0, y, z1, c);
		v(p, vc, cam, x1, y, z1, c); v(p, vc, cam, x1, y, z0, c);
	}

	/** 縦の壁。(x0,z0)-(x1,z1) の線に沿って、床 fy から WALL_H まで。 */
	private static void vQuad(PoseStack.Pose p, VertexConsumer vc, Vec3 cam, double x0, double z0, double x1, double z1, double fy, int lo, int hi) {
		v(p, vc, cam, x0, fy, z0, lo); v(p, vc, cam, x1, fy, z1, lo);
		v(p, vc, cam, x1, fy + WALL_H, z1, hi); v(p, vc, cam, x0, fy + WALL_H, z0, hi);
	}

	// ---- 射程ガイド ----

	private static void drawGuide(PoseStack.Pose p, VertexConsumer vc, Vec3 cam, Vec3 feet, float yawDeg, double range, boolean sup) {
		double r = Math.toRadians(yawDeg);
		double dx = -Math.sin(r), dz = Math.cos(r);   // 向き（MC の yaw 規約）
		double px = -dz * 0.5, pz = dx * 0.5;          // 幅 1.0 の半分だけ横へ
		double y = feet.y + 0.05;
		float cr = 1f, cg = sup ? 0.85f : 1f, cb = sup ? 0.10f : 1f;
		double tip = Math.min(1.5, range);
		int body = argb(0.28f, cr, cg, cb), bright = argb(0.55f, cr, cg, cb);
		double mid = range - tip;
		strip(p, vc, cam, feet, dx, dz, px, pz, y, 0, mid, body, body);
		strip(p, vc, cam, feet, dx, dz, px, pz, y, mid, range, body, bright);
	}

	private static void strip(PoseStack.Pose p, VertexConsumer vc, Vec3 cam, Vec3 o, double dx, double dz, double px, double pz,
			double y, double d0, double d1, int c0, int c1) {
		if (d1 - d0 < 1e-3) return;
		v(p, vc, cam, o.x + dx * d0 - px, y, o.z + dz * d0 - pz, c0);
		v(p, vc, cam, o.x + dx * d0 + px, y, o.z + dz * d0 + pz, c0);
		v(p, vc, cam, o.x + dx * d1 + px, y, o.z + dz * d1 + pz, c1);
		v(p, vc, cam, o.x + dx * d1 - px, y, o.z + dz * d1 - pz, c1);
	}

	// ---- 共通 ----

	private static void v(PoseStack.Pose p, VertexConsumer vc, Vec3 cam, double x, double y, double z, int argb) {
		vc.addVertex(p, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(argb);
	}

	private static int argb(float a, float r, float g, float b) {
		return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
}
