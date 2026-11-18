package com.amaguri.brawl.client.hud;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amaguri.brawl.client.model.BrawlerModels;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * キャラの顔（テクスチャの head ボーンの正面を切り出して拡大）。
 * models/brawler/&lt;id&gt;.json の head の最初の cube（uv, size[w,h,d]）から、正面 = (u+d, v+d, w, h) を使う。
 * モデルの高さ（ブロック）も一緒に覚える（選択画面の3D表示の大きさ合わせ用）。
 */
public final class Faces {
	private Faces() {}

	/** u,v,w,h=顔の領域（テクセル）、tw,th=テクスチャ全体、height=モデルの高さ（ブロック）。 */
	public record Info(int u, int v, int w, int h, int tw, int th, float height) {}

	private static final Map<String, Optional<Info>> CACHE = new HashMap<>();
	private static ResourceManager cachedFor;

	public static Info info(String id) {
		ResourceManager rm = Minecraft.getInstance().getResourceManager();
		if (rm != cachedFor) { CACHE.clear(); cachedFor = rm; } // リロードで作り直す
		return CACHE.computeIfAbsent(id, k -> Optional.ofNullable(load(rm, k))).orElse(null);
	}

	private static Info load(ResourceManager rm, String id) {
		if (id == null || !id.matches("[a-z0-9_]+")) return null;
		Optional<Resource> res = rm.getResource(BrawlerModels.modelLocation(id));
		if (res.isEmpty()) return null;
		try (InputStream in = res.get().open(); InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
			int tw = 64, th = 64;
			if (root.has("texture_size")) {
				JsonArray ts = root.getAsJsonArray("texture_size");
				tw = ts.get(0).getAsInt(); th = ts.get(1).getAsInt();
			}
			float top = 0f;
			Info face = null;
			for (JsonElement be : root.getAsJsonArray("bones")) {
				JsonObject b = be.getAsJsonObject();
				if (!b.has("cubes")) continue;
				for (JsonElement ce : b.getAsJsonArray("cubes")) {
					JsonObject c = ce.getAsJsonObject();
					JsonArray from = c.getAsJsonArray("from"), size = c.getAsJsonArray("size");
					top = Math.max(top, from.get(1).getAsFloat() + size.get(1).getAsFloat());
				}
				if (face == null && "head".equals(b.get("name").getAsString()) && b.getAsJsonArray("cubes").size() > 0) {
					JsonObject c = b.getAsJsonArray("cubes").get(0).getAsJsonObject();
					JsonArray size = c.getAsJsonArray("size"), uv = c.getAsJsonArray("uv");
					int w = Math.round(size.get(0).getAsFloat()), h = Math.round(size.get(1).getAsFloat());
					int d = Math.round(size.get(2).getAsFloat());
					face = new Info(uv.get(0).getAsInt() + d, uv.get(1).getAsInt() + d, w, h, tw, th, 0f);
				}
			}
			if (face == null) return null;
			return new Info(face.u, face.v, face.w, face.h, tw, th, top / 16f);
		} catch (Exception e) {
			return null;
		}
	}

	/** 顔を size×size の枠に縦横比を保って収めて描く（枠は描かない）。 */
	public static void draw(GuiGraphicsExtractor g, String id, int x, int y, int size) {
		Info f = info(id);
		if (f == null) { // 無いとき: 灰色の四角
			g.fill(x, y, x + size, y + size, 0xFF6B7391);
			return;
		}
		float s = Math.min((float) size / f.w, (float) size / f.h);
		int dw = Math.max(1, Math.round(f.w * s)), dh = Math.max(1, Math.round(f.h * s));
		g.blit(RenderPipelines.GUI_TEXTURED, BrawlerModels.textureLocation(id), x + (size - dw) / 2, y + (size - dh) / 2,
				(float) f.u, (float) f.v, dw, dh, f.w, f.h, f.tw, f.th);
	}

	/** 縁つきの顔。bg は背景（空き部分）の色、edge は縁の色。 */
	public static void framed(GuiGraphicsExtractor g, String id, int x, int y, int size, int bg, int edge, int bw) {
		Ui.box(g, x, y, size, size, bg, edge, bw);
		draw(g, id, x, y, size);
	}
}
