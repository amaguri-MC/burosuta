package com.amaguri.brawl.client.hud;

import java.util.List;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * 試合中のHUD（本家風）。座標は GUI 座標（W×H）。agent/preview/U/make_preview.py の match.png と同じ配置。
 *  上中央   : 斜めの帯に ドクロ＋「残り N」、その下に毒ガスの残り秒
 *  左上     : キルログ
 *  左下     : 自分の顔＋キャラ名＋キューブ数
 *  下中央   : HP（大きな数字）とバー、弾3本、必殺が満タンのとき「R 必殺技！」
 *  右下     : 必殺ボタン（大きな円）と攻撃ボタン（小さめの円）
 *  中央     : 3・2・1・ファイト！ / 脱落「#5 敗退」
 */
final class MatchHud {
	private MatchHud() {}

	static void vignettes(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
		Net.MatchPayload m = HudState.matchOrNull();
		// 被弾: 画面の縁が一瞬赤く
		long age = HudState.tick - HudState.hitFlashTick;
		if (age >= 0 && age < 12) Ui.vignette(g, w, h, 0xFF3A28, 1f - age / 12f);
		// 毒ガスの中: 縁が緑
		if (m != null && m.phase() == 2 && inGas(mc, m)) {
			float pulse = 0.75f + 0.25f * (float) Math.sin(HudState.tick * 0.3);
			Ui.vignette(g, w, h, 0x50E030, pulse);
		}
	}

	private static boolean inGas(Minecraft mc, Net.MatchPayload m) {
		double px = mc.player.getX(), pz = mc.player.getZ();
		return px < m.gasMinX() || px > m.gasMaxX() || pz < m.gasMinZ() || pz > m.gasMaxZ();
	}

	static void render(GuiGraphicsExtractor g, Font font, int w, int h, Net.HudPayload hud, Net.MatchPayload m) {
		int phase = m == null ? 0 : m.phase();
		topBand(g, font, w, m);
		killFeed(g, font);
		if (hud.alive()) {
			portrait(g, font, h, hud);
			hpAndAmmo(g, font, w, h, hud);
			buttons(g, font, w, h, hud);
		}
		center(g, font, w, h, m, phase);
	}

	// ---- 上中央 ----

	static void topBand(GuiGraphicsExtractor g, Font font, int w, Net.MatchPayload m) {
		if (m == null || m.phase() < 1) return;
		int cx = w / 2;
		Ui.panel(g, cx - 53, 4, 100, 22, 6, Ui.NAVY, Ui.NAVY_EDGE, 2);
		String label = Ui.t("brawl.hud.remaining");
		String num = String.valueOf(m.alive());
		int lw = font.width(label), nw = Ui.width(font, num, 2f);
		int total = 16 + 3 + lw + 3 + nw;
		int x = cx - total / 2;
		Ui.icon(g, "skull", x, 7, 16);
		Ui.text(g, font, label, x + 19, 12, 1f, Ui.GREY, Ui.NAVY_EDGE, Ui.LEFT);
		Ui.text(g, font, num, x + 19 + lw + 3, 7, 2f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT, true);
		if (m.phase() == 2) gasLine(g, font, cx, m);
	}

	private static void gasLine(GuiGraphicsExtractor g, Font font, int cx, Net.MatchPayload m) {
		boolean coming = m.gasSecondsToNext() <= 0; // 縮み中
		String s = coming ? Ui.t("brawl.hud.gas_coming") : Ui.t("brawl.hud.gas_in", m.gasSecondsToNext());
		int a = 255;
		if (coming && (HudState.tick / 6) % 2 == 1) a = 80; // 点滅
		int tw = font.width(s);
		int pw = tw + 28;
		Ui.panel(g, cx - (pw + 4) / 2, 31, pw, 12, 4, coming ? Ui.alpha(Ui.RED_BG, 255) : 0xFF16401C,
				coming ? Ui.RED_EDGE : Ui.GAS_EDGE, 2);
		Ui.iconTint(g, "gas", cx - (pw + 4) / 2 + 4, 30, 14, Ui.alpha(Ui.WHITE, a));
		Ui.text(g, font, s, cx - (pw + 4) / 2 + 20, 33, 1f, Ui.alpha(coming ? Ui.YELLOW : Ui.GAS, a),
				Ui.alpha(coming ? Ui.RED_EDGE : Ui.GAS_EDGE, a), Ui.LEFT);
	}

	// ---- 左上: キルログ（最新4件、5秒で消える） ----

	static void killFeed(GuiGraphicsExtractor g, Font font) {
		List<HudState.Kill> ks = HudState.kills;
		int from = Math.max(0, ks.size() - 4);
		int row = 0;
		for (int i = from; i < ks.size(); i++) {
			HudState.Kill k = ks.get(i);
			long age = HudState.tick - k.tick();
			float slide = 0f; // 出るときと消えるとき 左へスライド
			if (age < 4) slide = (4 - age) / 4f;
			else if (age > HudState.KILL_LIFE - 8) slide = (age - (HudState.KILL_LIFE - 8)) / 8f;
			boolean gas = k.killer().isEmpty();
			int fs = 11;
			int kw = gas ? 12 : fs + 3 + font.width(k.killer());
			int vw = fs + 3 + font.width(k.victim());
			int xw = font.width("×");
			int total = kw + 5 + xw + 5 + vw;
			int pw = total + 10;
			int y = 6 + row * 17;
			int x = 6 - (int) (slide * (pw + 12));
			row++;
			Ui.panel(g, x, y, pw, 14, 4, Ui.NAVY, Ui.NAVY_EDGE, 2);
			int cx = x + 6;
			if (gas) Ui.icon(g, "gas", cx, y + 1, 12);
			else {
				Faces.framed(g, k.killerBrawler(), cx, y + 1, fs, Ui.NAVY_LIGHT, Ui.NAVY_EDGE, 1);
				Ui.text(g, font, k.killer(), cx + fs + 3, y + 3, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT);
			}
			cx += kw + 5;
			Ui.text(g, font, "×", cx, y + 3, 1f, Ui.RED, Ui.RED_EDGE, Ui.LEFT);
			cx += xw + 5;
			Faces.framed(g, k.victimBrawler(), cx, y + 1, fs, Ui.NAVY_LIGHT, Ui.NAVY_EDGE, 1);
			Ui.text(g, font, k.victim(), cx + fs + 3, y + 3, 1f, 0xFFFFC8C0, Ui.NAVY_EDGE, Ui.LEFT);
		}
	}

	// ---- 左下: 自分の顔・キャラ名・キューブ数 ----

	static void portrait(GuiGraphicsExtractor g, Font font, int h, Net.HudPayload hud) {
		int fx = 8, fy = h - 44, fs = 36;
		BrawlerDef def = Brawlers.get(hud.brawler());
		String name = def != null ? def.nameJa() : hud.brawler();
		Ui.panel(g, 44, h - 26, 64, 14, 4, Ui.BLUE, Ui.BLUE_EDGE, 2);
		Ui.text(g, font, name, 54, h - 23, 1f, Ui.WHITE, Ui.BLUE_EDGE, Ui.LEFT);
		Ui.panel(g, 44, h - 42, 38, 13, 4, 0xFF2A9A36, Ui.GREEN_EDGE, 2);
		Ui.icon(g, "cube", 49, h - 42, 12);
		Ui.text(g, font, String.valueOf(hud.cubes()), 64, h - 41, 1.25f, Ui.WHITE, Ui.GREEN_EDGE, Ui.LEFT, true);
		Ui.box(g, fx - 2, fy - 2, fs + 4, fs + 4, Ui.YELLOW, Ui.NAVY_EDGE, 2);
		g.fill(fx, fy, fx + fs, fy + fs, Ui.NAVY);
		Faces.draw(g, hud.brawler(), fx, fy, fs);
	}

	// ---- 下中央: HP・弾・必殺のうながし ----

	static void hpAndAmmo(GuiGraphicsExtractor g, Font font, int w, int h, Net.HudPayload hud) {
		int cx = w / 2;
		float frac = hud.maxHp() <= 0 ? 0f : Mth.clamp((float) hud.hp() / hud.maxHp(), 0f, 1f);
		boolean low = frac < 0.3f;
		Ui.text(g, font, String.valueOf(Math.max(0, hud.hp())), cx, h - 57, 2.5f, Ui.WHITE,
				low ? Ui.RED_EDGE : Ui.GREEN_EDGE, Ui.CENTER, true);
		int bw = 132;
		Ui.fatBar(g, cx - bw / 2, h - 30, bw, 12, frac, low ? Ui.RED : Ui.GREEN, Ui.GREEN_BG, Ui.NAVY_EDGE, 2);
		int n = Math.max(1, hud.maxAmmo()), sw = 40, gap = 6;
		int x0 = cx - (n * sw + (n - 1) * gap) / 2;
		for (int i = 0; i < n; i++) {
			float f = Mth.clamp(hud.ammo() - i, 0f, 1f);
			Ui.fatBar(g, x0 + i * (sw + gap), h - 14, sw, 7, f, Ui.ORANGE, Ui.ORANGE_BG, Ui.NAVY_EDGE, 2);
		}
		if (hud.superCharge() >= 0.999f) { // 必殺が満タン
			float pulse = 0.5f + 0.5f * (float) Math.sin(HudState.tick * 0.4);
			int pw = 92 + Math.round(pulse * 4);
			Ui.panel(g, cx - (pw + 5) / 2, h - 80, pw, 15, 5, Ui.YELLOW_DARK, Ui.YELLOW_EDGE, 2);
			Ui.text(g, font, Ui.t("brawl.hud.super_ready", HudClient.superKeyLabel()), cx, h - 77, 1.25f, Ui.WHITE,
					Ui.YELLOW_EDGE, Ui.CENTER, true);
		}
	}

	// ---- 右下: 必殺ボタン（大）と攻撃ボタン（小） ----

	static void buttons(GuiGraphicsExtractor g, Font font, int w, int h, Net.HudPayload hud) {
		float c = Mth.clamp(hud.superCharge(), 0f, 1f);
		boolean full = c >= 0.999f;
		int r = 26, cx = w - 38, cy = h - 38;
		float pulse = 0.5f + 0.5f * (float) Math.sin(HudState.tick * 0.45);
		if (full) { // 光って脈打つ
			Ui.disc(g, cx, cy, r + 5 + Math.round(pulse * 3), Ui.alpha(Ui.YELLOW, 70 + (int) (pulse * 70)));
		}
		Ui.disc(g, cx, cy, r + 3, Ui.NAVY_EDGE);
		Ui.disc(g, cx, cy, r + 1, full ? (pulse > 0.5f ? Ui.YELLOW : 0xFFFFF0A0) : Ui.NAVY_LIGHT);
		Ui.disc(g, cx, cy, r - 2, Ui.NAVY);
		Ui.discFill(g, cx, cy, r - 2, c, Ui.YELLOW_DARK, Ui.YELLOW);
		Ui.iconTint(g, "star", cx - 15, cy - 15, 30, full ? 0xFFFFFFFF : 0xB0FFFFFF);
		// キー表示（R）
		Ui.box(g, cx - r + 1, cy - r + 1, 13, 13, Ui.NAVY, Ui.NAVY_EDGE, 1);
		Ui.text(g, font, HudClient.superKeyLabel(), cx - r + 7, cy - r + 4, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.CENTER);

		int r2 = 18, ax = cx - r - r2 - 8, ay = h - 30;
		boolean ready = hud.ammo() >= 1f;
		Ui.disc(g, ax, ay, r2 + 3, Ui.NAVY_EDGE);
		Ui.disc(g, ax, ay, r2 + 1, ready ? Ui.BLUE_EDGE : Ui.NAVY_LIGHT);
		Ui.disc(g, ax, ay, r2 - 1, ready ? Ui.BLUE : 0xFF4A5578);
		Ui.disc(g, ax, ay - r2 / 2 - 1, r2 / 2, ready ? Ui.lighten(Ui.BLUE, 40) : 0xFF5A6588);
		Ui.disc(g, ax, ay + 3, r2 - 5, ready ? Ui.BLUE : 0xFF4A5578);
		Ui.icon(g, "click", ax - 11, ay - 11, 22);
	}

	// ---- 中央: カウントダウン・ファイト！・脱落 ----

	static void center(GuiGraphicsExtractor g, Font font, int w, int h, Net.MatchPayload m, int phase) {
		int cx = w / 2;
		if (phase == 1 && m != null && m.countdown() > 0) { // 3・2・1: 拡大→縮小
			float t = (HudState.tick - HudState.countdownStart) / 10f;
			float scale = 7f * (1f + 0.6f * (1f - Ui.easeOut(t)));
			bigCentered(g, font, String.valueOf(m.countdown()), cx, h / 2 - 8, scale, 255);
		}
		long fa = HudState.tick - HudState.fightStart;
		if (phase == 2 && fa >= 0 && fa < 34) { // ファイト！
			float scale = 4.5f * (1f + 0.8f * (1f - Ui.easeOut(fa / 8f)));
			int a = (int) (255 * Mth.clamp((34 - fa) / 8f, 0f, 1f));
			bigCentered(g, font, Ui.t("brawl.hud.fight"), cx, h / 2 - 8, scale, a);
		}
		if (m != null && m.myRank() > 0) { // 脱落・勝利の表示（確定から4秒は大きく、観戦中は小さく上に）
			boolean win = m.myRank() == 1;
			String s = Ui.t(win ? "brawl.hud.win" : "brawl.hud.out", m.myRank());
			boolean big = phase == 3 || HudState.tick - HudState.rankStart < 80;
			int col = win ? Ui.YELLOW : Ui.WHITE, edge = win ? Ui.YELLOW_EDGE : Ui.RED_EDGE;
			if (big) {
				float pop = 1f + 0.5f * (1f - Ui.easeOut((HudState.tick - HudState.rankStart) / 8f));
				Ui.text(g, font, s, cx, h / 3 - (int) (2 * 4f * pop), 4f * pop, col, edge, Ui.CENTER, true);
			} else {
				Ui.panel(g, cx - 38, 48, 70, 18, 5, Ui.NAVY, Ui.NAVY_EDGE, 2);
				Ui.text(g, font, s, cx, 51, 1.75f, col, edge, Ui.CENTER, true);
			}
		}
	}

	/** 黄色・濃い縁・太い文字を画面中央（y は上端）に。 */
	static void bigCentered(GuiGraphicsExtractor g, Font font, String s, int cx, int cy, float scale, int a) {
		int ty = Math.round(cy - 4f * scale);
		Ui.text(g, font, s, cx, ty + Math.round(scale * 0.8f), scale, Ui.alpha(Ui.NAVY_EDGE, a / 2), Ui.alpha(Ui.NAVY_EDGE, a / 2), Ui.CENTER, true);
		Ui.text(g, font, s, cx, ty, scale, Ui.alpha(Ui.YELLOW, a), Ui.alpha(Ui.YELLOW_EDGE, a), Ui.CENTER, true);
	}
}
