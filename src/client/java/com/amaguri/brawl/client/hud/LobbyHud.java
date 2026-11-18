package com.amaguri.brawl.client.hud;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * ロビーの画面（HudState.lobby.inLobby が true の間。試合HUDは出さない）。agent/preview/U/make_preview.py の lobby.png と同じ配置。
 *  左上: トロフィー（金のトロフィー＋数）・勝利数 / 下中央: 選んでいるキャラの顔と名前・「B: キャラ選択」
 *  上中央: 準備の状態、カウントダウン中は大きく数字、自分が未準備なら「プレイ床に乗って準備完了！」
 */
final class LobbyHud {
	private LobbyHud() {}

	static void render(GuiGraphicsExtractor g, Font font, int w, int h, Net.LobbyPayload l) {
		int cx = w / 2;

		// 左上: トロフィーと勝利数
		Ui.panel(g, 12, 7, 66, 18, 5, Ui.NAVY, Ui.NAVY_EDGE, 2);
		Ui.icon(g, "trophy", 4, 3, 24);
		Ui.text(g, font, String.valueOf(l.trophies()), 32, 10, 1.5f, Ui.YELLOW, Ui.YELLOW_EDGE, Ui.LEFT, true);
		Ui.panel(g, 12, 30, 56, 14, 4, Ui.NAVY, Ui.NAVY_EDGE, 2);
		Ui.icon(g, "crown", 6, 28, 16);
		Ui.text(g, font, Ui.t("brawl.hud.wins", l.wins()), 26, 33, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT);

		// 上中央: 準備の状態
		String ready = Ui.t("brawl.hud.ready", l.ready(), l.total());
		int rw = font.width(ready) * 5 / 4 + 24;
		Ui.panel(g, cx - (rw + 5) / 2, 5, rw, 18, 5, l.meReady() ? 0xFF2A9A36 : Ui.NAVY,
				l.meReady() ? Ui.GREEN_EDGE : Ui.NAVY_EDGE, 2);
		Ui.text(g, font, ready, cx, 10, 1.25f, Ui.WHITE, l.meReady() ? Ui.GREEN_EDGE : Ui.NAVY_EDGE, Ui.CENTER, true);

		if (l.countdown() >= 0) { // 開始カウントダウン: 大きな数字
			String lab = Ui.t("brawl.hud.starting");
			Ui.text(g, font, lab, cx, 28, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.CENTER);
			float t = (HudState.tick - HudState.lobbyCountStart) / 10f;
			float scale = 6f * (1f + 0.5f * (1f - Ui.easeOut(t)));
			MatchHud.bigCentered(g, font, String.valueOf(l.countdown()), cx, 44 + 24, scale, 255);
		} else if (!l.meReady()) { // 未準備: うながし
			String s = Ui.t("brawl.hud.step_on");
			int bob = Math.round((float) Math.sin(HudState.tick * 0.2) * 1.5f);
			int pw = font.width(s) + 16;
			Ui.panel(g, cx - (pw + 4) / 2, 28 + bob, pw, 14, 4, Ui.BLUE, Ui.BLUE_EDGE, 2);
			Ui.text(g, font, s, cx, 31 + bob, 1f, Ui.WHITE, Ui.BLUE_EDGE, Ui.CENTER);
		}

		// 下中央: 選んでいるキャラ
		String id = l.selected();
		BrawlerDef def = Brawlers.get(id);
		String name = def != null ? def.nameJa() : id;
		int fx = cx - 62, fy = h - 46;
		Ui.panel(g, cx - 28, h - 42, 92, 16, 5, Ui.BLUE, Ui.BLUE_EDGE, 2);
		Ui.text(g, font, name, cx - 18, h - 38, 1.25f, Ui.WHITE, Ui.BLUE_EDGE, Ui.LEFT, true);
		Ui.panel(g, cx - 28, h - 24, 92, 14, 4, Ui.NAVY, Ui.NAVY_EDGE, 2);
		Ui.box(g, cx - 22, h - 22, 12, 11, Ui.YELLOW, Ui.YELLOW_EDGE, 1);
		Ui.text(g, font, HudClient.selectKeyLabel(), cx - 16, h - 20, 1f, Ui.NAVY_EDGE, Ui.YELLOW, Ui.CENTER);
		Ui.text(g, font, Ui.t("brawl.hud.select_hint"), cx - 6, h - 21, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT);
		Ui.box(g, fx - 2, fy - 2, 38, 38, Ui.YELLOW, Ui.NAVY_EDGE, 2);
		g.fill(fx, fy, fx + 34, fy + 34, Ui.NAVY);
		Faces.draw(g, id, fx, fy, 34);
	}
}
