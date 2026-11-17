package com.amaguri.brawl.client.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * models/brawler/&lt;id&gt;.json（tools/MODEL_FORMAT.md）を読んで ModelPart の木にする。結果（無い場合も）をキャッシュ。
 * 座標変換: 形式は x+=キャラの右・y上向き・足元y=0・正面-Z。ModelPart は y下向き・足元y=24。
 * model = (-x, 24-y, z)。回転は X・Y を逆符号、Z はそのまま（ZYX の順は同じ）。
 */
public final class BrawlerModels {
	private BrawlerModels() {
	}

	private static final Map<String, Optional<BrawlerModel>> CACHE = new ConcurrentHashMap<>();

	public static Identifier modelLocation(String id) {
		return Identifier.fromNamespaceAndPath("brawl", "models/brawler/" + id + ".json");
	}

	public static Identifier textureLocation(String id) {
		return Identifier.fromNamespaceAndPath("brawl", "textures/entity/brawler/" + id + ".png");
	}

	public static void clearCache() {
		CACHE.clear();
	}

	/** 無ければ null。 */
	public static BrawlerModel get(String id) {
		Optional<BrawlerModel> m = CACHE.get(id);
		if (m == null) {
			m = Optional.ofNullable(load(id));
			CACHE.put(id, m);
		}
		return m.orElse(null);
	}

	private static BrawlerModel load(String id) {
		if (!id.matches("[a-z0-9_]+")) {
			return null;
		}
		ResourceManager rm = Minecraft.getInstance().getResourceManager();
		Optional<Resource> res = rm.getResource(modelLocation(id));
		if (res.isEmpty() || rm.getResource(textureLocation(id)).isEmpty()) {
			return null;
		}
		try (InputStream in = res.get().open(); InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return parse(id, JsonParser.parseReader(r).getAsJsonObject());
		} catch (Exception e) {
			LogUtils.getLogger().error("[brawl] キャラモデル {} を読めませんでした", id, e);
			return null;
		}
	}

	private static final class Bone {
		String name;
		String parent;
		float px, py, pz;
		float rx, ry, rz; // 度（形式の座標）
		JsonArray cubes;
		final List<Bone> children = new ArrayList<>();
	}

	static BrawlerModel parse(String id, JsonObject root) {
		int tw = 64;
		int th = 64;
		if (root.has("texture_size")) {
			JsonArray ts = root.getAsJsonArray("texture_size");
			tw = ts.get(0).getAsInt();
			th = ts.get(1).getAsInt();
		}
		Map<String, Bone> bones = new LinkedHashMap<>();
		for (JsonElement e : root.getAsJsonArray("bones")) {
			JsonObject o = e.getAsJsonObject();
			Bone b = new Bone();
			b.name = o.get("name").getAsString();
			b.parent = o.has("parent") && !o.get("parent").isJsonNull() ? o.get("parent").getAsString() : null;
			float[] p = vec(o, "pivot");
			float[] r = vec(o, "rotation");
			b.px = p[0];
			b.py = p[1];
			b.pz = p[2];
			b.rx = r[0];
			b.ry = r[1];
			b.rz = r[2];
			b.cubes = o.has("cubes") ? o.getAsJsonArray("cubes") : new JsonArray();
			bones.put(b.name, b);
		}
		List<Bone> tops = new ArrayList<>();
		for (Bone b : bones.values()) {
			Bone par = b.parent == null ? null : bones.get(b.parent);
			if (par == null || par == b) {
				tops.add(b);
			} else {
				par.children.add(b);
			}
		}
		MeshDefinition mesh = new MeshDefinition();
		Map<String, String[]> paths = new HashMap<>();
		for (Bone b : tops) {
			build(mesh.getRoot(), null, b, new String[0], paths);
		}
		ModelPart baked = LayerDefinition.create(mesh, tw, th).bakeRoot();
		Map<String, ModelPart> parts = new HashMap<>();
		for (Map.Entry<String, String[]> en : paths.entrySet()) {
			ModelPart p = baked;
			for (String s : en.getValue()) {
				p = p.getChild(s);
			}
			parts.put(en.getKey(), p);
		}
		return new BrawlerModel(baked, parts, id);
	}

	private static void build(PartDefinition parentDef, Bone parent, Bone b, String[] parentPath, Map<String, String[]> paths) {
		// 位置は親の pivot からの差（ModelPart 座標）。親の回転は子にも掛かる（形式と同じ意味）
		float ox = -b.px;
		float oy = 24f - b.py;
		float oz = b.pz;
		if (parent != null) {
			ox -= -parent.px;
			oy -= 24f - parent.py;
			oz -= parent.pz;
		}
		float d2r = (float) (Math.PI / 180.0);
		PartPose pose = PartPose.offsetAndRotation(ox, oy, oz, -b.rx * d2r, -b.ry * d2r, b.rz * d2r);
		CubeListBuilder cb = CubeListBuilder.create();
		for (JsonElement ce : b.cubes) {
			JsonObject c = ce.getAsJsonObject();
			float[] f = vec(c, "from");
			float[] s = vec(c, "size");
			JsonArray uv = c.getAsJsonArray("uv");
			float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
			boolean mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
			// ModelPart 座標でこの bone の pivot からの最小角
			float rx = b.px - f[0] - s[0];
			float ry = b.py - f[1] - s[1];
			float rz = f[2] - b.pz;
			cb.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).mirror(mirror)
					.addBox(rx, ry, rz, s[0], s[1], s[2], new CubeDeformation(inflate));
		}
		PartDefinition def = parentDef.addOrReplaceChild(b.name, cb, pose);
		String[] path = new String[parentPath.length + 1];
		System.arraycopy(parentPath, 0, path, 0, parentPath.length);
		path[parentPath.length] = b.name;
		paths.put(b.name, path);
		for (Bone c : b.children) {
			build(def, b, c, path, paths);
		}
	}

	private static float[] vec(JsonObject o, String key) {
		float[] v = new float[3];
		if (o.has(key)) {
			JsonArray a = o.getAsJsonArray(key);
			for (int i = 0; i < 3; i++) {
				v[i] = a.get(i).getAsFloat();
			}
		}
		return v;
	}
}
