package com.amaguri.brawl.client.view;

import com.amaguri.brawl.client.hud.HudState;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** V view: 本家風の見下ろし視点の状態・カメラ・照準の計算。クライアントのメインスレッドだけで使う。 */
public final class TopDown {
	private TopDown() {}

	public static final float YAW = 180f;        // 北向き固定
	public static final float PITCH = 58f;       // 俯角
	public static final double DISTANCE = 11.5;  // 注視点からカメラまで
	public static final double AIM_HEIGHT = 1.0; // 足元からの照準面の高さ

	/** キー V で切り替える（既定ON）。 */
	static boolean enabled = true;
	private static float aimYaw = 0f;
	private static Vec3 aimPoint = null;
	private static Vec3 focus = null;
	private static long lastNanos = 0;

	/** 見下ろし中か。 */
	public static boolean active() {
		if (!enabled) return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.gui.screen() != null) return false;
		return HudState.alive() || (HudState.lobby != null && HudState.lobby.inLobby());
	}

	public static float aimYaw() { return aimYaw; }

	/** カーソルが指す地面（高さ 足元+1.0 の面）の点。無ければ null。 */
	public static Vec3 aimPoint() { return aimPoint; }

	// ---- 数式（agent/preview/V/ray_check.py と同じ式） ----

	/** カメラの前方ベクトル（MC の yaw/pitch 規約）。 */
	public static Vec3 forward(float yawDeg, float pitchDeg) {
		double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
		return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
	}

	/** 画面の左方向（MC の leftVector 相当）。 */
	public static Vec3 left(float yawDeg) {
		double y = Math.toRadians(yawDeg);
		return new Vec3(Math.cos(y), 0, Math.sin(y));
	}

	/** nx, ny は 0..1（左上原点）のカーソル位置。ワールドの光線の向き（単位ベクトル）。 */
	public static Vec3 rayDir(float yawDeg, float pitchDeg, double fovYDeg, double aspect, double nx, double ny) {
		Vec3 f = forward(yawDeg, pitchDeg);
		Vec3 l = left(yawDeg);
		Vec3 up = f.cross(l);
		double t = Math.tan(Math.toRadians(fovYDeg) / 2.0);
		double sx = (2 * nx - 1) * t * aspect; // 右が正
		double sy = (1 - 2 * ny) * t;          // 上が正
		return f.add(l.scale(-sx)).add(up.scale(sy)).normalize();
	}

	/** 光線と水平面 y=planeY の交点。交わらなければ null。 */
	public static Vec3 intersectPlane(Vec3 origin, Vec3 dir, double planeY) {
		if (dir.y > -1e-4) return null;
		double k = (planeY - origin.y) / dir.y;
		if (k < 0) return null;
		return origin.add(dir.scale(k));
	}

	/** 毎フレーム Camera.update の最後から呼ぶ。カメラ位置を決め、照準を更新し、プレイヤーの向きを合わせる。 */
	public static void onCameraUpdate(Camera cam, float partial, CameraSetter setter) {
		if (!active()) { focus = null; aimPoint = null; return; }
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		Vec3 feet = p.getPosition(partial);
		Vec3 target = new Vec3(feet.x, feet.y + AIM_HEIGHT, feet.z);
		long now = System.nanoTime();
		double dt = lastNanos == 0 ? 0.016 : Math.min(0.25, (now - lastNanos) / 1.0e9);
		lastNanos = now;
		if (focus == null || focus.distanceToSqr(target) > 40 * 40) focus = target;
		else focus = focus.add(target.subtract(focus).scale(1 - Math.exp(-dt * 10.0)));
		Vec3 f = forward(YAW, PITCH);
		Vec3 camPos = focus.subtract(f.scale(DISTANCE));
		setter.set(camPos, YAW, PITCH);

		// 照準
		var win = mc.getWindow();
		double w = Math.max(1, win.getScreenWidth()), h = Math.max(1, win.getScreenHeight());
		double nx = mc.mouseHandler.xpos() / w, ny = mc.mouseHandler.ypos() / h;
		double fov = cam.getFov();
		Vec3 dir = rayDir(YAW, PITCH, fov, (double) win.getWidth() / Math.max(1, win.getHeight()), nx, ny);
		Vec3 hit = intersectPlane(camPos, dir, feet.y + AIM_HEIGHT);
		aimPoint = hit;
		if (hit != null) {
			double dx = hit.x - feet.x, dz = hit.z - feet.z;
			if (dx * dx + dz * dz > 1e-4) aimYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		}
		// プレイヤーの向きを照準へ（補間で回らないよう Old も揃える）
		p.setYRot(aimYaw); p.yRotO = aimYaw;
		p.setXRot(0f); p.xRotO = 0f;
		p.yHeadRot = aimYaw; p.yHeadRotO = aimYaw;
		p.yBodyRot = aimYaw; p.yBodyRotO = aimYaw;
	}

	public interface CameraSetter { void set(Vec3 pos, float yaw, float pitch); }

	/** 入力の移動ベクトル(左右=strafeLeft, 前後=forward)を画面基準(W=北)に回して、プレイヤーの向き基準の値にする。 */
	public static float[] rotateInput(float strafeLeft, float forward, float playerYawDeg) {
		double dx = -strafeLeft, dz = -forward; // A=西(-x), W=北(-z)
		double y = Math.toRadians(playerYawDeg);
		double s = Math.sin(y), c = Math.cos(y);
		double f = dx * -s + dz * c;
		double l = dx * c + dz * s;
		return new float[] { (float) l, (float) f };
	}
}
