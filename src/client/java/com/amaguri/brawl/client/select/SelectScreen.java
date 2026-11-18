package com.amaguri.brawl.client.select;

import java.util.List;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.client.hud.Faces;
import com.amaguri.brawl.client.hud.HudState;
import com.amaguri.brawl.client.hud.Ui;
import com.amaguri.brawl.client.model.BrawlerModel;
import com.amaguri.brawl.client.model.BrawlerModels;
import com.amaguri.brawl.client.model.MobPuppets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * キャラ選択画面。キー B（キー設定「キャラ選択」）または OpenSelectPayload で開く。
 * 左に5人の顔の一覧（クリックで切替）、中央に選択中のキャラの3Dモデル（ゆっくり回転・ドラッグで回せる）、
 * 右に名前・役割・HP・通常攻撃と必殺技の説明、下に大きな「選ぶ」ボタン（PickPayload を送って閉じる）。
 * 配置は agent/preview/U/make_preview.py の select.png と同じ。
 */
public final class SelectScreen extends Screen {
	private final List<BrawlerDef> defs = Brawlers.all();
	private int sel = 0;
	private final String current; // いま選んでいるキャラ（印を付ける）
	private int age = 0;
	private int switchAge = 99;
	private float dragYaw = 0f;

	// レイアウト（init で計算）
	private int ts, listX, listY, panelX, panelW, modelX0, modelX1, btnX, btnY, btnW = 112, btnH = 26;
	private static final int PANEL_W = 164;

	/** 3D表示用の Model.Simple（モデルが作り直されたら作り直す）。 */
	private static String simpleId = null;
	private static BrawlerModel simpleSrc = null;
	private static Model.Simple simple = null;

	public SelectScreen(String focus) {
		super(Component.translatable("brawl.select.title"));
		String cur = HudState.lobby != null ? HudState.lobby.selected() : "";
		this.current = cur == null ? "" : cur;
		String f = (focus == null || focus.isEmpty()) ? this.current : focus;
		for (int i = 0; i < defs.size(); i++) if (defs.get(i).id().equals(f)) sel = i;
	}

	@Override
	public boolean isPauseScreen() { return false; }

	@Override
	protected void init() {
		ts = Mth.clamp((this.height - 36) / 5 - 4, 20, 36);
		listX = 10; listY = 26;
		panelW = PANEL_W;
		panelX = this.width - panelW - 8;
		modelX0 = listX + ts + 14;
		modelX1 = panelX - 8;
		btnX = (modelX0 + modelX1) / 2 - btnW / 2;
		btnY = this.height - 34;
	}

	@Override
	public void tick() { age++; switchAge++; }

	private String id() { return defs.isEmpty() ? "" : defs.get(sel).id(); }

	private void select(int i) {
		if (i < 0 || i >= defs.size() || i == sel) return;
		sel = i; switchAge = 0; dragYaw = 0f;
		click(1.3f);
	}

	private void pick() {
		if (defs.isEmpty()) return;
		ClientPlayNetworking.send(new Net.PickPayload(id()));
		click(1.0f);
		onClose();
	}

	private void click(float pitch) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
	}

	// ---- 入力 ----

	private int tileY(int i) { return listY + i * (ts + 4); }

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
		if (e.button() != 0) return super.mouseClicked(e, dbl);
		for (int i = 0; i < defs.size(); i++) {
			if (Ui.in(e.x(), e.y(), listX - 3, tileY(i) - 3, ts + 6, ts + 6)) { select(i); return true; }
		}
		if (Ui.in(e.x(), e.y(), btnX, btnY, btnW, btnH)) { pick(); return true; }
		return super.mouseClicked(e, dbl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (Ui.in(e.x(), e.y(), modelX0, 14, modelX1 - modelX0, this.height - 70)) { dragYaw += (float) dx * 1.2f; return true; }
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		int k = e.key();
		if (k == Ui.KEY_ESC || k == Ui.KEY_B) { onClose(); return true; }
		if (k == Ui.KEY_ENTER) { pick(); return true; }
		if (k == Ui.KEY_UP || k == Ui.KEY_LEFT) { select(sel - 1); return true; }
		if (k == Ui.KEY_DOWN || k == Ui.KEY_RIGHT) { select(sel + 1); return true; }
		return super.keyPressed(e);
	}

	// ---- 描画 ----

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
		g.fillGradient(0, 0, this.width, this.height, 0xFF24408C, 0xFF0C1535);
		slant(g, modelX0 - 6, 0, 70, this.height, 40, 0x18FFFFFF);
		slant(g, modelX0 + 80, 0, 26, this.height, 40, 0x12FFFFFF);
		slant(g, modelX1 - 60, 0, 40, this.height, 40, 0x10FFFFFF);
	}

	private static void slant(GuiGraphicsExtractor g, int x, int y, int w, int h, int skew, int col) {
		for (int r = 0; r < h; r++) {
			int off = Math.round(skew * (float) (h - 1 - r) / Math.max(1, h - 1));
			g.fill(x + off, y + r, x + off + w, y + r + 1, col);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
		int w = this.width, h = this.height;
		super.extractRenderState(g, mx, my, pt);
		if (defs.isEmpty()) return;
		BrawlerDef def = defs.get(sel);
		String id = def.id();

		// タイトル
		Ui.panel(g, 10, 5, 84, 14, 5, Ui.YELLOW_DARK, Ui.YELLOW_EDGE, 2);
		Ui.text(g, this.font, Ui.t("brawl.select.title"), 18, 8, 1f, Ui.WHITE, Ui.YELLOW_EDGE, Ui.LEFT);

		// 左: 顔の一覧
		for (int i = 0; i < defs.size(); i++) {
			String bid = defs.get(i).id();
			int y = tileY(i);
			boolean on = i == sel, hover = Ui.in(mx, my, listX - 3, y - 3, ts + 6, ts + 6);
			if (on) Ui.box(g, listX - 3, y - 3, ts + 6, ts + 6, Ui.YELLOW, Ui.NAVY_EDGE, 2);
			else Ui.box(g, listX - 2, y - 2, ts + 4, ts + 4, hover ? Ui.NAVY_LIGHT : Ui.NAVY, Ui.NAVY_EDGE, 2);
			g.fill(listX, y, listX + ts, y + ts, on ? Ui.NAVY_LIGHT : Ui.NAVY);
			Faces.draw(g, bid, listX, y, ts);
			if (bid.equals(current)) Ui.icon(g, "star", listX + ts - 9, y - 4, 12); // いま使っているキャラ
		}

		// 中央: 3Dモデル（ゆっくり回転）と台座
		int mcx = (modelX0 + modelX1) / 2;
		int top = 16, bot = h - 52;
		platform(g, mcx, bot - 2, Math.min(60, (modelX1 - modelX0) / 2 - 6), 9);
		model3d(g, id, modelX0, top, modelX1, bot, pt);

		// 名前の札（モデルの下）
		float pop = 1f + 0.35f * (1f - Ui.easeOut((switchAge + pt) / 6f));
		int nw = Math.max(70, this.font.width(def.nameJa()) * 2 + 24);
		Ui.panel(g, mcx - (nw + 6) / 2, bot + 2, nw, 3, 3, Ui.YELLOW, Ui.YELLOW_EDGE, 1);

		// 右: 情報パネル
		info(g, def, pop);

		// 下: 「選ぶ」ボタン
		boolean hov = Ui.in(mx, my, btnX, btnY, btnW, btnH);
		int bf = hov ? Ui.lighten(Ui.GREEN, 24) : Ui.GREEN;
		Ui.panel(g, btnX, btnY, btnW, btnH, 7, bf, Ui.GREEN_EDGE, 3);
		Ui.text(g, this.font, Ui.t("brawl.select.pick"), btnX + btnW / 2 + 3, btnY + 5, 2f, Ui.WHITE, Ui.GREEN_EDGE, Ui.CENTER, true);
	}

	private static void platform(GuiGraphicsExtractor g, int cx, int cy, int a, int b) {
		for (int dy = -b; dy <= b; dy++) {
			int half = (int) (a * Math.sqrt(1.0 - (double) dy * dy / (b * b)));
			int edge = Math.abs(dy) >= b - 2 ? 0xFF0B1330 : 0xFF2C3A6B;
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, edge);
		}
		for (int dy = -b + 3; dy <= b - 3; dy++) {
			int half = (int) ((a - 3) * Math.sqrt(1.0 - (double) dy * dy / ((b - 3) * (b - 3))));
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, 0xFF3C4F94);
		}
	}

	/** キャラのモデルを GUI に描く（Model.Simple に包んで GuiGraphicsExtractor.skin で描く）。無ければ顔を大きく。 */
	private void model3d(GuiGraphicsExtractor g, String id, int x0, int y0, int x1, int y1, float pt) {
		// バニラのモブの人形（EntityRenderState を GUI に描く）
		if (MobPuppets.has(id)) {
			float rot = 180f + 25f + (age + pt) * 1.4f + dragYaw;
			EntityRenderState st = MobPuppets.guiState(Minecraft.getInstance().getEntityRenderDispatcher(), id, rot, age + pt);
			if (st != null) {
				float vh = MobPuppets.visualHeight(id);
				float px = 0.9f * (y1 - y0) / vh; // 1ブロックあたりのピクセル
				float tilt = (float) Math.toRadians(-5.0);
				// UNVERIFIED: entity() の引数は (state, 1ブロックのpx, 平行移動(ブロック), 回転, カメラ角, x0, y0, x1, y1)。InventoryScreen に倣い Z を PI 回して上下を直す
				g.entity(st, px, new Vector3f(0f, vh / 2f, 0f), new Quaternionf().rotateZ((float) Math.PI).rotateX(tilt),
						new Quaternionf().rotateX(tilt), x0, y0, x1, y1);
				return;
			}
		}
		BrawlerModel m = BrawlerModels.get(id);
		Faces.Info fi = Faces.info(id);
		if (m == null || fi == null) { // 代替: 正面の顔を大きく
			int s = Math.min(x1 - x0, y1 - y0) * 3 / 4;
			Faces.framed(g, id, (x0 + x1) / 2 - s / 2, (y0 + y1) / 2 - s / 2, s, Ui.NAVY, Ui.YELLOW, 3);
			return;
		}
		if (simple == null || !id.equals(simpleId) || simpleSrc != m) {
			m.resetPose();
			simple = new Model.Simple(m.root(), tex -> RenderTypes.entityCutout(tex));
			simpleId = id; simpleSrc = m;
		}
		m.resetPose();
		float hBlocks = Math.max(1f, fi.height());
		float scale = 0.9f * (y1 - y0) / hBlocks;
		float rotY = 25f + (age + pt) * 1.4f + dragYaw;
		// UNVERIFIED: skin() の引数順は (model, texture, scale, rotX, rotY, pivotY, x0, y0, x1, y1)（PlayerSkinWidget に倣う）
		g.skin(simple, BrawlerModels.textureLocation(id), scale, -5f, rotY, -hBlocks / 2f, x0, y0, x1, y1);
	}

	private void info(GuiGraphicsExtractor g, BrawlerDef def, float pop) {
		int px = panelX, py = 8, pw = panelW, ph = this.height - 16;
		Ui.panel(g, px, py, pw, ph, 0, 0xE0182454, Ui.NAVY_EDGE, 2);
		int x = px + 8, iw = pw - 16, y = py + 7;
		String id = def.id();

		// 名前と役割
		Ui.text(g, this.font, def.nameJa(), x, y - Math.round((pop - 1f) * 6f), 2f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT, true);
		y += 21;
		int rw = this.font.width(def.roleJa()) + 14;
		Ui.panel(g, x, y, rw, 11, 3, Ui.YELLOW_DARK, Ui.YELLOW_EDGE, 1);
		Ui.text(g, this.font, def.roleJa(), x + 6, y + 2, 1f, Ui.WHITE, Ui.YELLOW_EDGE, Ui.LEFT);
		y += 16;

		// HP（バーの長さは全キャラの最大HPとの比）
		int maxAll = 1;
		for (BrawlerDef d : defs) maxAll = Math.max(maxAll, d.maxHp());
		Ui.fatBar(g, x, y, iw, 10, (float) def.maxHp() / maxAll, Ui.GREEN, Ui.GREEN_BG, Ui.NAVY_EDGE, 2);
		Ui.text(g, this.font, "HP " + def.maxHp(), x + 4, y + 1, 1f, Ui.WHITE, Ui.GREEN_EDGE, Ui.LEFT, true);
		y += 16;

		// 数値（2列×2行）
		int cw = iw / 2 - 2;
		String[] stats = {
				Ui.t("brawl.select.stat.speed", fmt(def.speedTiles())),
				Ui.t("brawl.select.stat.range", fmt(def.attackRangeTiles())),
				Ui.t("brawl.select.stat.reload", fmt(def.reloadSec())),
				Ui.t("brawl.select.stat.super", Math.max(1, Math.round(1f / Math.max(0.01f, def.superPerHit()))))
		};
		for (int i = 0; i < 4; i++) {
			int sx = x + (i % 2) * (cw + 4), sy = y + (i / 2) * 14;
			Ui.panel(g, sx, sy, cw - 3, 11, 3, Ui.NAVY_LIGHT, Ui.NAVY_EDGE, 1);
			Ui.text(g, this.font, stats[i], sx + 5, sy + 2, 1f, Ui.WHITE, Ui.NAVY_EDGE, Ui.LEFT);
		}
		y += 32;

		// 通常攻撃・必殺技
		y = skill(g, "click", Ui.t("brawl.select.normal"), "brawl.select." + id + ".normal", x, y, iw, Ui.BLUE);
		y += 3;
		skill(g, "star", Ui.t("brawl.select.super"), "brawl.select." + id + ".super", x, y, iw, Ui.YELLOW_DARK);
	}

	/** 技の見出し（アイコン＋種別＋技名）と説明（折り返し）。次の y を返す。 */
	private int skill(GuiGraphicsExtractor g, String icon, String kind, String key, int x, int y, int iw, int col) {
		Ui.panel(g, x, y, iw - 4, 12, 4, col, Ui.darken(col, 80), 1);
		Ui.icon(g, icon, x + 2, y - 2, 14);
		String nm = Ui.t(key + ".name");
		Ui.text(g, this.font, kind, x + 20, y + 2, 1f, Ui.WHITE, Ui.darken(col, 80), Ui.LEFT);
		Ui.text(g, this.font, nm, x + 22 + this.font.width(kind), y + 2, 1f, Ui.YELLOW == col ? Ui.WHITE : 0xFFFFF0A0, Ui.darken(col, 80), Ui.LEFT);
		y += 15;
		List<FormattedCharSequence> lines = this.font.split(FormattedText.of(Ui.t(key + ".desc")), iw);
		for (FormattedCharSequence ln : lines) {
			g.text(this.font, ln, x, y, 0xFFE6ECFF, true);
			y += 9;
		}
		return y;
	}

	private static String fmt(double v) {
		return String.format(java.util.Locale.ROOT, "%.1f", v);
	}
}
