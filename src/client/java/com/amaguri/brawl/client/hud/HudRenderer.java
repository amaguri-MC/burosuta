package com.amaguri.brawl.client.hud;

import com.amaguri.brawl.api.Net;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** 試合中のHUD描画。機能UI優先: 縁取り文字と細いバーだけで、カードや背景パネルは使わない。 */
final class HudRenderer {
	private HudRenderer() {}

	// 色（0xAARRGGBB）。縁は隣の色を暗くした色相
	static final int GREEN = 0xFF4CD04A, GREEN_BG = 0xFF17301A, GREEN_EDGE = 0xFF1B6B24;
	static final int RED = 0xFFE5453B, RED_BG = 0xFF381614, RED_EDGE = 0xFF8A1E1A;
	static final int AMMO = 0xFFF4A81C, AMMO_EDGE = 0xFF8A5A0C, AMMO_BG = 0xFF332410;
	static final int CUBE = 0xFFB864F2, CUBE_EDGE = 0xFF5C2A8E;
	static final int WHITE = 0xFFFFFFFF, TEXT_EDGE = 0xFF141414;
	static final int GOLD = 0xFFFFD83A, GOLD_EDGE = 0xFF8A6A0A;
	static final int GAS = 0xFF6DE04A, GAS_EDGE = 0xFF1E5A14;

	static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		if (!HudState.active() || mc.player == null || mc.level == null) return;
		Font font = mc.font;
		int w = g.guiWidth(), h = g.guiHeight();
		Net.HudPayload hud = HudState.hud;
		Net.MatchPayload m = HudState.matchOrNull();
		int phase = m == null ? 0 : m.phase();

		gasVignette(g, mc, m, w, h);
		overheadBars(g, mc, font, dt, w, h);

		if (hud.alive()) {
			selfHp(g, font, hud, w, h);
			ammo(g, hud, w, h);
			superGauge(g, font, hud, w - 40, h - 46);
		}
		topInfo(g, font, hud, m, w);

		if (phase == 1 && m.countdown() > 0) {
			bigText(g, font, String.valueOf(m.countdown()), w / 2, h / 2 - 50, 7f, GOLD, GOLD_EDGE);
		}
		if (m != null && m.myRank() > 0) {
			float s = phase == 3 ? 6f : 4f;
			bigText(g, font, "#" + m.myRank(), w / 2, h / 3 - 10, s, m.myRank() == 1 ? GOLD : WHITE,
					m.myRank() == 1 ? GOLD_EDGE : TEXT_EDGE);
		}
	}

	// ---- 自分のHP・弾・必殺技 ----

	private static void selfHp(GuiGraphicsExtractor g, Font font, Net.HudPayload hud, int w, int h) {
		int bw = 120, bh = 7;
		int x = w / 2 - bw / 2, y = h - 44;
		bigText(g, font, String.valueOf(Math.max(0, hud.hp())), w / 2, h - 63, 2f, WHITE, GREEN_EDGE);
		bar(g, x, y, bw, bh, hud.maxHp() <= 0 ? 0f : (float) hud.hp() / hud.maxHp(), GREEN, GREEN_BG, GREEN_EDGE);
	}

	private static void ammo(GuiGraphicsExtractor g, Net.HudPayload hud, int w, int h) {
		int n = Math.max(1, hud.maxAmmo());
		int sw = 36, gap = 6, sh = 5;
		int total = n * sw + (n - 1) * gap;
		int x0 = w / 2 - total / 2, y = h - 34;
		for (int i = 0; i < n; i++) {
			float f = Mth.clamp(hud.ammo() - i, 0f, 1f);
			bar(g, x0 + i * (sw + gap), y, sw, sh, f, f >= 1f ? AMMO : 0xFFC98A18, AMMO_BG, AMMO_EDGE);
		}
	}

	/** 右下の丸ゲージ。満タンで光って「R」。 */
	private static void superGauge(GuiGraphicsExtractor g, Font font, Net.HudPayload hud, int cx, int cy) {
		float c = Mth.clamp(hud.superCharge(), 0f, 1f);
		boolean full = c >= 0.999f;
		int outer = 22, inner = 16;
		float pulse = 0.5f + 0.5f * (float) Math.sin(HudState.tick * 0.45);
		int fill = full ? GOLD : 0xFFE8B02A;
		int edge = full ? GOLD_EDGE : 0xFF6E5012;
		int back = 0xFF2A2412;
		if (full) { // 光る輪
			int a = 0x40 + (int) (0x60 * pulse);
			ring(g, cx, cy, outer + 4, outer + 1, 2f, (a << 24) | 0x00FFE060, (a << 24) | 0x00FFE060, edge, back);
		}
		ring(g, cx, cy, outer, inner, c, fill, fill, edge, back);
		if (full) {
			bigText(g, font, HudClient.superKeyLabel(), cx, cy - 8, 2f, WHITE, GOLD_EDGE);
		} else {
			bigText(g, font, String.valueOf((int) (c * 100)), cx, cy - 4, 1f, WHITE, TEXT_EDGE);
		}
		bigText(g, font, "必殺技", cx, cy + outer + 4, 1f, full ? GOLD : WHITE, full ? GOLD_EDGE : TEXT_EDGE);
	}

	// ---- 上中央 ----

	private static void topInfo(GuiGraphicsExtractor g, Font font, Net.HudPayload hud, Net.MatchPayload m, int w) {
		int y = 6;
		if (m != null && m.phase() >= 1) {
			bigText(g, font, "残り " + m.alive(), w / 2, y, 2f, WHITE, TEXT_EDGE);
		}
		y += 20;
		cubeIcon(g, w / 2 - 14, y + 4);
		String cs = String.valueOf(hud.cubes());
		outlined(g, font, cs, w / 2 - 6, y, CUBE, CUBE_EDGE);
		y += 12;
		if (m != null && m.phase() == 2) {
			String s = m.gasSecondsToNext() > 0 ? "毒ガスまで " + m.gasSecondsToNext() + "秒" : "毒ガス縮小中";
			bigText(g, font, s, w / 2, y, 1f, GAS, GAS_EDGE);
		}
	}

	/** 毒ガスの中にいるとき、画面の縁を緑に。 */
	private static void gasVignette(GuiGraphicsExtractor g, Minecraft mc, Net.MatchPayload m, int w, int h) {
		if (m == null || m.phase() != 2) return;
		double px = mc.player.getX(), pz = mc.player.getZ();
		boolean inGas = px < m.gasMinX() || px > m.gasMaxX() || pz < m.gasMinZ() || pz > m.gasMaxZ();
		if (!inGas) return;
		float pulse = 0.75f + 0.25f * (float) Math.sin(HudState.tick * 0.3);
		int e = Math.max(18, Math.min(w, h) / 10);
		for (int i = 0; i < e; i++) {
			float t = 1f - (float) i / e;
			int a = (int) (0x90 * t * t * pulse);
			int col = (a << 24) | 0x0050E030;
			g.fill(i, i, w - i, i + 1, col);                 // 上
			g.fill(i, h - i - 1, w - i, h - i, col);         // 下
			g.fill(i, i + 1, i + 1, h - i - 1, col);         // 左
			g.fill(w - i - 1, i + 1, w - i, h - i - 1, col); // 右
		}
	}

	// ---- 頭上HPバー（実体の位置を画面に射影してHUDに描く） ----

	private static void overheadBars(GuiGraphicsExtractor g, Minecraft mc, Font font, DeltaTracker dt, int w, int h) {
		Net.BarsPayload b = HudState.barsOrNull();
		if (b == null || mc.level == null) return;
		float pt = dt.getGameTimeDeltaPartialTick(false);
		int n = Math.min(b.ids().length, Math.min(b.hp().length, Math.min(b.maxHp().length,
				Math.min(b.team().length, b.cubes().length))));
		for (int i = 0; i < n; i++) {
			Entity e = mc.level.getEntity(b.ids()[i]);
			if (e == null || e.isRemoved() || !e.isAlive() || b.hp()[i] <= 0) continue;
			if (HudState.isHidden(e)) continue; // 隠れた実体は出さない
			double x = Mth.lerp(pt, e.xo, e.getX());
			double y = Mth.lerp(pt, e.yo, e.getY()) + e.getBbHeight() + 0.45;
			double z = Mth.lerp(pt, e.zo, e.getZ());
			// UNVERIFIED: GameRenderer.projectPointToScreen は NDC（x,y が -1〜1、z>1 で背面）を返すと仮定
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
			if (ndc == null || ndc.z > 1.0 || ndc.z < -1.0 || Math.abs(ndc.x) > 1.1 || Math.abs(ndc.y) > 1.1) continue;
			int sx = (int) ((ndc.x + 1.0) * 0.5 * w);
			int sy = (int) ((1.0 - ndc.y) * 0.5 * h);
			boolean self = b.team()[i] == 0;
			int col = self ? GREEN : RED, bg = self ? GREEN_BG : RED_BG, edge = self ? GREEN_EDGE : RED_EDGE;
			int bw = 40, bh = 4;
			float f = b.maxHp()[i] <= 0 ? 0f : (float) b.hp()[i] / b.maxHp()[i];
			bar(g, sx - bw / 2, sy - bh, bw, bh, f, col, bg, edge);
			bigText(g, font, String.valueOf(b.hp()[i]), sx, sy - bh - 11, 1f, WHITE, edge);
			if (b.cubes()[i] > 0) {
				String cs = String.valueOf(b.cubes()[i]);
				int tw = font.width(cs);
				cubeIcon(g, sx - bw / 2 - 5 - tw - 4, sy - 3);
				outlined(g, font, cs, sx - bw / 2 - 2 - tw, sy - 8, CUBE, CUBE_EDGE);
			}
		}
	}

	// ---- 部品 ----

	/** 細いバー: 暗い縁1px、背景、塗り。 */
	static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int col, int bg, int edge) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, edge);
		g.fill(x, y, x + w, y + h, bg);
		int fw = Math.round(w * Mth.clamp(frac, 0f, 1f));
		if (fw > 0) {
			g.fill(x, y, x + fw, y + h, col);
			if (h >= 4) g.fill(x, y, x + fw, y + 1, lighten(col)); // 上端の細いハイライト
		}
	}

	static int lighten(int c) {
		int r = Math.min(255, ((c >> 16) & 255) + 40), gg = Math.min(255, ((c >> 8) & 255) + 40), b = Math.min(255, (c & 255) + 40);
		return (c & 0xFF000000) | (r << 16) | (gg << 8) | b;
	}

	/** 縁取り文字（左上基準）。 */
	static void outlined(GuiGraphicsExtractor g, Font font, String s, int x, int y, int col, int edge) {
		g.text(font, s, x - 1, y, edge, false);
		g.text(font, s, x + 1, y, edge, false);
		g.text(font, s, x, y - 1, edge, false);
		g.text(font, s, x, y + 1, edge, false);
		g.text(font, s, x - 1, y - 1, edge, false);
		g.text(font, s, x + 1, y - 1, edge, false);
		g.text(font, s, x - 1, y + 1, edge, false);
		g.text(font, s, x + 1, y + 1, edge, false);
		g.text(font, s, x, y, col, false);
	}

	/** 縁取り文字を横中央（cx）・上端 y に scale 倍で描く。 */
	static void bigText(GuiGraphicsExtractor g, Font font, String s, int cx, int y, float scale, int col, int edge) {
		int tw = font.width(s);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, y);
		pose.scale(scale, scale);
		outlined(g, font, s, -tw / 2, 0, col, edge);
		pose.popMatrix();
	}

	/** 小さな菱形（パワーキューブの印）。中心 (cx,cy)。 */
	static void cubeIcon(GuiGraphicsExtractor g, int cx, int cy) {
		for (int dy = -4; dy <= 4; dy++) {
			int half = 4 - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, CUBE_EDGE);
		}
		for (int dy = -3; dy <= 3; dy++) {
			int half = 3 - Math.abs(dy);
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, CUBE);
		}
	}

	/**
	 * 丸ゲージ。上から時計回りに frac（0〜1）だけ fill 色。frac>1 なら全部 fill 色。
	 * 行ごとに同じ種類のピクセルをまとめて fill する。
	 */
	static void ring(GuiGraphicsExtractor g, int cx, int cy, int outer, int inner, float frac, int fill, int fillB,
			int edge, int back) {
		float o2 = outer * outer, i2 = inner * inner;
		float oe = (outer - 1) * (outer - 1), ie = (inner + 1) * (inner + 1);
		for (int dy = -outer; dy <= outer; dy++) {
			int runStart = 0, runKind = 0;
			for (int dx = -outer; dx <= outer + 1; dx++) {
				int kind = 0;
				if (dx <= outer) {
					float r2 = dx * dx + dy * dy;
					if (r2 <= o2 && r2 >= i2) {
						if (r2 > oe || r2 < ie) kind = 1; // 縁
						else {
							double a = Math.atan2(dx, -dy);
							if (a < 0) a += Math.PI * 2;
							kind = (a / (Math.PI * 2) <= frac) ? 2 : 3;
						}
					}
				}
				if (kind != runKind) {
					if (runKind != 0) {
						int col = runKind == 1 ? edge : runKind == 2 ? fill : back;
						g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, col);
					}
					runStart = dx;
					runKind = kind;
				}
			}
		}
	}
}
