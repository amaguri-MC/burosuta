package com.amaguri.brawl.client.hud;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 本家風の「ゲームのUI」部品（試合HUD・ロビー・選択画面・結果画面で共通）。
 * 太い濃色の縁取り、斜めの帯、大きな縁取り文字。色は 0xAARRGGBB。縁は黒でなく隣の色を暗くした色相。
 * agent/preview/U/make_preview.py が同じ式で見本を描く（式を変えたら両方直す）。
 */
public final class Ui {
	private Ui() {}

	public static final int NAVY = 0xFF1E2B52, NAVY_EDGE = 0xFF0B1330, NAVY_LIGHT = 0xFF34457F;
	public static final int YELLOW = 0xFFFFD21F, YELLOW_EDGE = 0xFF7A4A00, YELLOW_DARK = 0xFFE0A100;
	public static final int BLUE = 0xFF2F8CFF, BLUE_EDGE = 0xFF123C8A;
	public static final int GREEN = 0xFF4CD04A, GREEN_EDGE = 0xFF1B6B24, GREEN_BG = 0xFF14301A;
	public static final int RED = 0xFFFF4A3D, RED_EDGE = 0xFF7A1410, RED_BG = 0xFF3A1412;
	public static final int ORANGE = 0xFFFF9A1F, ORANGE_EDGE = 0xFF8A4A0A, ORANGE_BG = 0xFF3A2410;
	public static final int WHITE = 0xFFFFFFFF, GREY = 0xFFB8C2DC;
	public static final int GAS = 0xFF6DE04A, GAS_EDGE = 0xFF1E5A14;

	public static final int LEFT = -1, CENTER = 0, RIGHT = 1;

	// ---- 色 ----

	public static int lighten(int c, int n) {
		int r = Math.min(255, ((c >> 16) & 255) + n), g = Math.min(255, ((c >> 8) & 255) + n), b = Math.min(255, (c & 255) + n);
		return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	public static int darken(int c, int n) { return lighten(c, -n); }

	public static int alpha(int c, int a) { return (Mth.clamp(a, 0, 255) << 24) | (c & 0xFFFFFF); }

	// ---- 図形 ----

	/** 斜めの帯（平行四辺形）。左下が (x,y+h) で、上の行ほど右へずれる（最大 skew）。edge の太さ bw。 */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int skew, int fill, int edge, int bw) {
		float k = h > 1 ? (float) skew / (h - 1) : 0f;
		for (int r = -bw; r < h + bw; r++) { // 影
			int off = Math.round(k * (h - 1 - r));
			g.fill(x + off - bw, y + r + 2, x + off + w + bw, y + r + 3, 0x55000000);
		}
		for (int r = -bw; r < h + bw; r++) { // 縁
			int off = Math.round(k * (h - 1 - r));
			g.fill(x + off - bw, y + r, x + off + w + bw, y + r + 1, edge);
		}
		for (int r = 0; r < h; r++) { // 面（上は明るく下は暗く）
			int off = Math.round(k * (h - 1 - r));
			int c = r < 2 ? lighten(fill, 28) : (r >= h - 2 ? darken(fill, 22) : fill);
			g.fill(x + off, y + r, x + off + w, y + r + 1, c);
		}
	}

	/** 斜めでない角丸なしの縁つき四角。 */
	public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int edge, int bw) {
		g.fill(x - bw, y - bw, x + w + bw, y + h + bw, edge);
		g.fill(x, y, x + w, y + h, fill);
		if (h >= 4) g.fill(x, y, x + w, y + 1, lighten(fill, 28));
	}

	/** 縁つきの横バー。frac だけ塗る。 */
	public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int fill, int bg, int edge) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, edge);
		g.fill(x, y, x + w, y + h, bg);
		int fw = Math.round(w * Mth.clamp(frac, 0f, 1f));
		if (fw > 0) {
			g.fill(x, y, x + fw, y + h, fill);
			if (h >= 4) {
				g.fill(x, y, x + fw, y + 1, lighten(fill, 50));
				g.fill(x, y + h - 1, x + fw, y + h, darken(fill, 30));
			}
		}
	}

	/** 太い縁（bw）つきの横バー。 */
	public static void fatBar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int fill, int bg, int edge, int bw) {
		g.fill(x - bw, y - bw, x + w + bw, y + h + bw, edge);
		bar(g, x, y, w, h, frac, fill, bg, bg);
	}

	/** 塗りつぶしの円（行ごとの fill）。 */
	public static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int col) {
		for (int dy = -r; dy <= r; dy++) {
			int half = (int) Math.floor(Math.sqrt(Math.max(0, r * r + r - dy * dy)));
			g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, col);
		}
	}

	/** 円の下から level（0〜1）まで col で満たす（円の内側だけ）。 */
	public static void discFill(GuiGraphicsExtractor g, int cx, int cy, int r, float level, int col, int colTop) {
		int top = Math.round(cy + r - (2 * r + 1) * Mth.clamp(level, 0f, 1f));
		for (int dy = -r; dy <= r; dy++) {
			int y = cy + dy;
			if (y < top) continue;
			int half = (int) Math.floor(Math.sqrt(Math.max(0, r * r + r - dy * dy)));
			g.fill(cx - half, y, cx + half + 1, y + 1, y <= top + 1 ? colTop : col);
		}
	}

	/** 画面の縁を col で染める（中心ほど透明）。strength 0〜1。 */
	public static void vignette(GuiGraphicsExtractor g, int w, int h, int rgb, float strength) {
		int e = Math.max(14, Math.min(w, h) / 6);
		for (int i = 0; i < e; i++) {
			float t = 1f - (float) i / e;
			int a = (int) (0xB0 * t * t * strength);
			if (a <= 0) continue;
			int col = (a << 24) | (rgb & 0xFFFFFF);
			g.fill(i, i, w - i, i + 1, col);
			g.fill(i, h - i - 1, w - i, h - i, col);
			g.fill(i, i + 1, i + 1, h - i - 1, col);
			g.fill(w - i - 1, i + 1, w - i, h - i - 1, col);
		}
	}

	// ---- アイコン（textures/gui/<name>.png 32x32） ----

	public static Identifier tex(String name) {
		return Identifier.fromNamespaceAndPath("brawl", "textures/gui/" + name + ".png");
	}

	public static void icon(GuiGraphicsExtractor g, String name, int x, int y, int size) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex(name), x, y, 0f, 0f, size, size, 32, 32, 32, 32);
	}

	/** 色（0xAARRGGBB）で乗算して描くアイコン（薄くしたいときなど）。 */
	public static void iconTint(GuiGraphicsExtractor g, String name, int x, int y, int size, int argb) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex(name), x, y, 0f, 0f, size, size, 32, 32, 32, 32, argb);
	}

	/** 言語ファイルの文字列（引数つき）。 */
	public static String t(String key, Object... args) {
		return net.minecraft.network.chat.Component.translatable(key, args).getString();
	}

	// ---- 文字 ----

	private static boolean wide(String s) {
		for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 0x2000) return true;
		return false;
	}

	/** 縁取り文字（左上基準、glyph座標）。日本語は小さいと縁で潰れるので 2倍以上でだけ細い縁、それ未満は影だけ。 */
	private static void outlinedRaw(GuiGraphicsExtractor g, Font font, String s, int col, int edge, boolean bold, float t) {
		if (t <= 0f) { // 影だけ
			var pose = g.pose();
			pose.pushMatrix(); pose.translate(1f, 1f);
			g.text(font, s, 0, 0, edge, false);
			pose.popMatrix();
		} else {
			float[] d = { -t, 0, t, 0, 0, -t, 0, t, -t, -t, t, -t, -t, t, t, t };
			var pose = g.pose();
			for (int i = 0; i < d.length; i += 2) {
				pose.pushMatrix(); pose.translate(d[i], d[i + 1]);
				g.text(font, s, 0, 0, edge, false);
				pose.popMatrix();
			}
			if (bold) { // 縁の下側にも少し太らせる
				pose.pushMatrix(); pose.translate(0.5f, 0f);
				g.text(font, s, 0, 0, edge, false);
				pose.popMatrix();
			}
		}
		g.text(font, s, 0, 0, col, false);
		if (bold) {
			var pose = g.pose();
			pose.pushMatrix(); pose.translate(0.5f, 0f);
			g.text(font, s, 0, 0, col, false);
			pose.popMatrix();
		}
	}

	/** 縁取り文字。align=LEFT/CENTER/RIGHT で x を基準に、y は上端。scale 倍。 */
	public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, float scale, int col, int edge, int align, boolean bold) {
		float tw = font.width(s) * scale;
		float ax = align == CENTER ? x - tw / 2f : align == RIGHT ? x - tw : x;
		boolean jp = wide(s);
		float t = !jp ? (scale >= 3f ? 0.75f : 1f) : (scale >= 2f ? 0.6f : 0f);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(ax, y);
		pose.scale(scale, scale);
		outlinedRaw(g, font, s, col, edge, bold && !jp, t);
		pose.popMatrix();
	}

	public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, float scale, int col, int edge, int align) {
		text(g, font, s, x, y, scale, col, edge, align, false);
	}

	/** 文字の幅（scale 倍）。 */
	public static int width(Font font, String s, float scale) {
		return Math.round(font.width(s) * scale);
	}

	// ---- 操作 ----

	public static String keyLabel(net.minecraft.client.KeyMapping k, String fallback) {
		return k == null ? fallback : k.getTranslatedKeyMessage().getString();
	}

	public static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** GLFW のキー番号（InputConstants に頼らず定数で持つ）。 */
	public static final int KEY_B = InputConstants.KEY_B, KEY_ESC = InputConstants.KEY_ESCAPE,
			KEY_ENTER = InputConstants.KEY_RETURN, KEY_LEFT = InputConstants.KEY_LEFT, KEY_RIGHT = InputConstants.KEY_RIGHT,
			KEY_UP = InputConstants.KEY_UP, KEY_DOWN = InputConstants.KEY_DOWN;

	/** 0→1 の ease-out（拡大→縮小などの演出用）。 */
	public static float easeOut(float t) {
		t = Mth.clamp(t, 0f, 1f);
		return 1f - (1f - t) * (1f - t);
	}
}
