package com.amaguri.brawl.client.hud;

import com.amaguri.brawl.api.Net;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 結果画面（ResultPayload）。画面全体の半透明パネルに 大きく「#1 勝利！」「#4」、トロフィー増減（上がる演出）、
 * 全員の順位表（順位・キャラ顔・名前）。8秒、またはクリック/Esc で閉じる。agent/preview/U/make_preview.py の result.png と同じ配置。
 */
public final class ResultScreen extends Screen {
	private static final int LIFE = 160; // 8秒

	private final Net.ResultPayload r;
	private int age = 0;

	public ResultScreen(Net.ResultPayload r) {
		super(Component.translatable("brawl.result.title"));
		this.r = r;
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	public void tick() {
		if (++age >= LIFE) onClose();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		onClose();
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
		g.fill(0, 0, this.width, this.height, 0xC0101A38); // 画面全体の半透明パネル
		boolean win = r.rank() == 1;
		// 順位を載せる斜めの帯
		Ui.panel(g, -10, this.height / 2 - 56, this.width / 2 + 40, 64, 16, win ? 0xFF6A4A00 : 0xFF1F4FA8,
				win ? Ui.YELLOW_EDGE : Ui.BLUE_EDGE, 2);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
		int w = this.width, h = this.height;
		float t = age + pt;
		boolean win = r.rank() == 1;

		// 左: 大きな順位表示
		int lx = (w / 2 + 24) / 2 - 6 + 8; // 左半分の中心
		String head = win ? Ui.t("brawl.hud.win", 1) : "#" + r.rank();
		float base = win ? 5f : 7f;
		float pop = 1f + 0.8f * (1f - Ui.easeOut(t / 8f));
		float scale = base * pop;
		int ty = h / 2 - 24 - Math.round(4f * scale);
		Ui.text(g, this.font, head, lx, ty + Math.round(scale * 0.8f), scale, Ui.alpha(Ui.NAVY_EDGE, 120), Ui.alpha(Ui.NAVY_EDGE, 120), Ui.CENTER, true);
		Ui.text(g, this.font, head, lx, ty, scale, win ? Ui.YELLOW : Ui.WHITE, win ? Ui.YELLOW_EDGE : Ui.BLUE_EDGE, Ui.CENTER, true);
		Ui.text(g, this.font, Ui.t("brawl.result.of", r.total()), lx, h / 2 + 16, 1f, Ui.GREY, Ui.NAVY_EDGE, Ui.CENTER);

		// 左: トロフィー増減（数字が上がる演出）
		float tt = Mth.clamp((t - 14f) / 26f, 0f, 1f);
		int shown = Math.round(r.trophyDelta() * Ui.easeOut(tt));
		String ds = (r.trophyDelta() > 0 ? "+" : "") + shown;
		int dcol = r.trophyDelta() > 0 ? 0xFF7CF070 : r.trophyDelta() < 0 ? 0xFFFF6A5E : Ui.GREY;
		int dedge = r.trophyDelta() > 0 ? Ui.GREEN_EDGE : r.trophyDelta() < 0 ? Ui.RED_EDGE : Ui.NAVY_EDGE;
		int rise = Math.round(10f * (1f - Ui.easeOut(tt)));
		int dy = h / 2 + 38 - rise;
		int dw = Ui.width(this.font, ds, 3f);
		int total = 28 + 4 + dw;
		int dx = lx - total / 2;
		if (t > 8f) {
			Ui.panel(g, dx - 10, dy - 4, total + 16, 30, 6, Ui.NAVY, Ui.NAVY_EDGE, 2);
			Ui.icon(g, "trophy", dx, dy - 1, 28);
			Ui.text(g, this.font, ds, dx + 32, dy + 1, 3f, dcol, dedge, Ui.LEFT, true);
			Ui.text(g, this.font, Ui.t("brawl.result.trophies", r.trophies()), lx, dy + 33, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.CENTER);
		}

		// 右: 全員の順位表
		int tx = w / 2 + 30, tw = w / 2 - 44;
		int n = r.names().length;
		int rh = Math.max(11, Math.min(18, (h - 50) / Math.max(1, n)));
		int ty0 = 22;
		Ui.panel(g, tx, 6, tw, 12, 4, Ui.NAVY, Ui.NAVY_EDGE, 2);
		Ui.text(g, this.font, Ui.t("brawl.result.table"), tx + 8, 8, 1f, Ui.GREY, Ui.NAVY_EDGE, Ui.LEFT);
		String me = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getName().getString() : "";
		int mine = -1;
		for (int i = 0; i < n; i++) if (r.ranks()[i] == r.rank() && r.names()[i].equals(me)) mine = i;
		if (mine < 0) for (int i = 0; i < n; i++) if (r.ranks()[i] == r.rank()) { mine = i; break; }
		for (int i = 0; i < n; i++) {
			float ap = Mth.clamp((t - 4f - i * 2f) / 6f, 0f, 1f);
			if (ap <= 0f) continue;
			int y = ty0 + i * (rh + 2);
			int x = tx - Math.round((1f - Ui.easeOut(ap)) * 40f);
			boolean me2 = i == mine;
			Ui.panel(g, x, y, tw, rh, 4, me2 ? 0xFF2F6FD8 : Ui.NAVY, me2 ? Ui.YELLOW : Ui.NAVY_EDGE, 2);
			int rank = r.ranks()[i];
			int rc = rank == 1 ? Ui.YELLOW : rank == 2 ? 0xFFD8DEEE : rank == 3 ? 0xFFE0A070 : Ui.WHITE;
			Ui.text(g, this.font, "#" + rank, x + 6, y + (rh - 8) / 2 + 1, 1f, rc, Ui.NAVY_EDGE, Ui.LEFT, true);
			int fs = rh - 3;
			Faces.framed(g, r.brawlers()[i], x + 30, y + 1, fs, Ui.NAVY_LIGHT, Ui.NAVY_EDGE, 1);
			Ui.text(g, this.font, r.names()[i], x + 34 + fs, y + (rh - 8) / 2 + 1, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT);
		}

		// 下: とじる案内
		Ui.text(g, this.font, Ui.t("brawl.result.close"), w / 2, h - 12, 1f, Ui.GREY, Ui.NAVY_EDGE, Ui.CENTER);
	}
}
