package com.amaguri.brawl.client.hud;

import com.amaguri.brawl.api.Net;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** HUDの入口。試合中は MatchHud、ロビーは LobbyHud、それ以外は何も出さない。頭上表示とダメージ数字はここ。 */
final class HudRenderer {
	private HudRenderer() {}

	static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		boolean match = HudState.inMatch(), lobby = HudState.inLobby();
		if (!match && !lobby) return; // 試合外・ロビー外では何も出さない
		Font font = mc.font;
		int w = g.guiWidth(), h = g.guiHeight();

		if (match) MatchHud.vignettes(g, mc, w, h);
		if (HudState.active() && (HudState.statusFlags(mc.player.getId()) & (HudState.ST_STUN | HudState.ST_FREEZE)) != 0) {
			Ui.vignette(g, w, h, 0x7FD8FF, 0.9f); // 自分がスタン・凍結中: 画面の縁が水色
		}
		if (HudState.active()) {
			overheadBars(g, mc, font, dt, w, h);
			damagePops(g, mc, font, dt, w, h);
		}
		if (match) MatchHud.render(g, font, w, h, HudState.hud, HudState.matchOrNull());
		else LobbyHud.render(g, font, w, h, HudState.lobby);
	}

	// ---- 頭上表示（実体の位置を画面に射影してHUDに描く）: 名前 → HP数字 → HPバー → 弾（自分だけ） ----

	static final int BAR_W = 50, BAR_H = 6;

	private static void overheadBars(GuiGraphicsExtractor g, Minecraft mc, Font font, DeltaTracker dt, int w, int h) {
		Net.BarsPayload b = HudState.barsOrNull();
		if (b == null || mc.level == null) return;
		boolean firstPerson = mc.options.getCameraType().isFirstPerson();
		float pt = dt.getGameTimeDeltaPartialTick(false);
		int n = Math.min(b.ids().length, Math.min(b.hp().length, Math.min(b.maxHp().length,
				Math.min(b.team().length, b.cubes().length))));
		for (int i = 0; i < n; i++) {
			boolean self = b.team()[i] == 0;
			boolean ally = b.team()[i] == 2; // 自分の召喚物（相棒）は青
			if (self && firstPerson) continue; // 自分の頭上表示は三人称（見下ろし含む）のときだけ
			Entity e = mc.level.getEntity(b.ids()[i]);
			if (e == null || e.isRemoved() || !e.isAlive() || b.hp()[i] <= 0) continue;
			if (HudState.isHidden(e)) continue; // 隠れた実体は出さない
			double x = Mth.lerp(pt, e.xo, e.getX());
			double y = Mth.lerp(pt, e.yo, e.getY()) + e.getBbHeight() + 0.55;
			double z = Mth.lerp(pt, e.zo, e.getZ());
			int[] sp = project(mc, x, y, z, w, h);
			if (sp == null) continue;
			int sx = sp[0], sy = sp[1]; // sy = バーの上端
			int fillCol = self ? Ui.GREEN : ally ? Ui.BLUE : Ui.RED, bg = self ? Ui.GREEN_BG : ally ? Ui.NAVY : Ui.RED_BG,
					edge = self ? Ui.GREEN_EDGE : ally ? Ui.BLUE_EDGE : Ui.RED_EDGE;
			float f = b.maxHp()[i] <= 0 ? 0f : (float) b.hp()[i] / b.maxHp()[i];
			Ui.fatBar(g, sx - BAR_W / 2, sy, BAR_W, BAR_H, f, fillCol, bg, Ui.NAVY_EDGE, 2);
			Ui.text(g, font, String.valueOf(b.hp()[i]), sx, sy - 14, 1.25f, Ui.WHITE, edge, Ui.CENTER, true);
			String name = e.getDisplayName().getString(); // ボットは名前タグのボット名、プレイヤーはプレイヤー名
			Ui.text(g, font, name, sx, sy - 25, 1f, self ? Ui.WHITE : 0xFFFFE4E0, Ui.NAVY_EDGE, Ui.CENTER);
			if (b.cubes()[i] > 0) { // 左に緑のキューブ印＋個数
				String cs = String.valueOf(b.cubes()[i]);
				int tw = font.width(cs);
				int ix = sx - BAR_W / 2 - 4 - tw - 11;
				Ui.icon(g, "cube", ix, sy - 3, 11);
				Ui.text(g, font, cs, ix + 12, sy - 1, 1f, Ui.WHITE, Ui.GREEN_EDGE, Ui.LEFT);
			}
			int below = sy + BAR_H + 4;
			int frost = HudState.frost(b.ids()[i]);
			if (frost > 0) { // 凍結ゲージ: HPバーの下に水色の細いバー
				Ui.fatBar(g, sx - BAR_W / 2, sy + BAR_H + 1, BAR_W, 3, Mth.clamp(frost / 100f, 0f, 1f), 0xFF7FD8FF, 0xFF1B3A5C, Ui.NAVY_EDGE, 1);
				below += 4;
			}
			if (self) {
				Net.HudPayload hud = HudState.hud;
				if (hud != null) {
					int cnt = Math.max(1, hud.maxAmmo()), gap = 3;
					int sw = (BAR_W - gap * (cnt - 1)) / cnt;
					int x0 = sx - (cnt * sw + gap * (cnt - 1)) / 2;
					for (int k = 0; k < cnt; k++) {
						float af = Mth.clamp(hud.ammo() - k, 0f, 1f);
						Ui.fatBar(g, x0 + k * (sw + gap), below, sw, 3, af, Ui.ORANGE, Ui.ORANGE_BG, Ui.NAVY_EDGE, 1);
					}
				}
				below += 8;
				skillPopup(g, font, sx, below);
			}
		}
	}

	/** 実体の位置を画面（GUI座標）へ。見えない・画面外なら null。 */
	private static int[] project(Minecraft mc, double x, double y, double z, int w, int h) {
		// UNVERIFIED: GameRenderer.projectPointToScreen は NDC（x,y が -1〜1、z>1 で背面）を返すと仮定
		Vec3 ndc = mc.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
		if (ndc == null || ndc.z > 1.0 || ndc.z < -1.0 || Math.abs(ndc.x) > 0.98 || Math.abs(ndc.y) > 0.98) return null;
		return new int[] { (int) ((ndc.x + 1.0) * 0.5 * w), (int) ((1.0 - ndc.y) * 0.5 * h) };
	}

	/** 技名を1秒ふわっと（少し上がりながら出て消える）。 */
	private static void skillPopup(GuiGraphicsExtractor g, Font font, int cx, int y) {
		String t = HudState.skillText;
		if (t == null) return;
		long age = HudState.tick - HudState.skillStart, left = HudState.skillLast + 20 - HudState.tick;
		if (left <= 0) return;
		float in = Mth.clamp(age / 4f, 0f, 1f), out = Mth.clamp(left / 6f, 0f, 1f);
		int a = (int) (255 * Math.min(in, out));
		if (a < 12) return;
		float rise = Mth.clamp(age / 10f, 0f, 1f) * 6f;
		boolean sup = HudState.skillSuper;
		Ui.text(g, font, t, cx, y + 6 - (int) rise, 1.25f, Ui.alpha(sup ? Ui.YELLOW : Ui.WHITE, a),
				Ui.alpha(sup ? Ui.YELLOW_EDGE : Ui.NAVY_EDGE, a), Ui.CENTER);
	}

	// ---- ダメージ数字: その実体の頭上に ふわっと上がって消える（敵は赤・自分が受けた分は白） ----

	private static void damagePops(GuiGraphicsExtractor g, Minecraft mc, Font font, DeltaTracker dt, int w, int h) {
		float pt = dt.getGameTimeDeltaPartialTick(false);
		for (HudState.Pop p : HudState.pops) {
			float age = HudState.tick - p.start + pt;
			if (age < 0 || age > HudState.POP_LIFE) continue;
			double x = p.x, y = p.y, z = p.z;
			Entity e = mc.level.getEntity(p.id);
			if (e != null && !e.isRemoved()) { x = e.getX(); y = e.getY() + e.getBbHeight() + 0.4; z = e.getZ(); }
			int[] sp = project(mc, x, y, z, w, h);
			if (sp == null) continue;
			float rise = 24f * Ui.easeOut(age / HudState.POP_LIFE);
			int a = (int) (255 * Mth.clamp((HudState.POP_LIFE - age) / 8f, 0f, 1f));
			if (a < 10) continue;
			float scale = 1.6f + 0.9f * (1f - Ui.easeOut(age / 5f));
			int col = p.self ? Ui.WHITE : 0xFFFF4A3D, edge = p.self ? Ui.NAVY_EDGE : Ui.RED_EDGE;
			Ui.text(g, font, String.valueOf(p.amount), sp[0] + (int) p.dx, sp[1] - 8 - (int) rise, scale,
					Ui.alpha(col, a), Ui.alpha(edge, a), Ui.CENTER, true);
		}
	}
}
