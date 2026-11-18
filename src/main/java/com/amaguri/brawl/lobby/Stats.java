package com.amaguri.brawl.lobby;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** トロフィー・勝利数・選択キャラ。config/brawl_stats.json に UUID→{name, trophies, wins, selected} で保存。サーバースレッド専用。 */
public final class Stats {
	private Stats() {}

	private static final Logger LOG = LoggerFactory.getLogger("brawl-lobby");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** 順位ごとのトロフィー増減（1位〜10位）。 */
	private static final int[] DELTA = {10, 8, 7, 6, 4, 2, 0, -1, -2, -3};

	public static final class Entry {
		public String name = "";
		public int trophies;
		public int wins;
		public String selected = "";
	}

	private static final Map<UUID, Entry> MAP = new LinkedHashMap<>();
	private static boolean loaded;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("brawl_stats.json");
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		Path f = file();
		if (!Files.exists(f)) return;
		try {
			JsonElement root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
			if (!root.isJsonObject()) return;
			for (Map.Entry<String, JsonElement> en : root.getAsJsonObject().entrySet()) {
				try {
					UUID id = UUID.fromString(en.getKey());
					JsonObject o = en.getValue().getAsJsonObject();
					Entry e = new Entry();
					if (o.has("name")) e.name = o.get("name").getAsString();
					if (o.has("trophies")) e.trophies = Math.max(0, o.get("trophies").getAsInt());
					if (o.has("wins")) e.wins = Math.max(0, o.get("wins").getAsInt());
					if (o.has("selected")) e.selected = o.get("selected").getAsString();
					MAP.put(id, e);
				} catch (RuntimeException ex) {
					LOG.warn("brawl_stats.json の1件を読めませんでした: {}", en.getKey());
				}
			}
		} catch (IOException | RuntimeException ex) {
			LOG.warn("brawl_stats.json を読めませんでした", ex);
		}
	}

	public static void save() {
		load();
		try {
			Path f = file();
			Files.createDirectories(f.getParent());
			JsonObject root = new JsonObject();
			for (Map.Entry<UUID, Entry> en : MAP.entrySet()) {
				Entry e = en.getValue();
				JsonObject o = new JsonObject();
				o.addProperty("name", e.name);
				o.addProperty("trophies", e.trophies);
				o.addProperty("wins", e.wins);
				o.addProperty("selected", e.selected);
				root.add(en.getKey().toString(), o);
			}
			Files.writeString(f, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			LOG.warn("brawl_stats.json を保存できませんでした", ex);
		}
	}

	/** 無ければ作る。name が空でなければ更新する。 */
	public static Entry get(UUID id, String name) {
		load();
		Entry e = MAP.computeIfAbsent(id, k -> new Entry());
		if (name != null && !name.isEmpty()) e.name = name;
		return e;
	}

	public static Entry get(UUID id) {
		return get(id, null);
	}

	/** 順位に対するトロフィー増減。 */
	public static int delta(int rank) {
		if (rank < 1) return 0;
		return DELTA[Math.min(rank, DELTA.length) - 1];
	}

	/** トロフィー上位 n 人（トロフィー降順、同数は勝利数の多い順）。0 トロフィーの人は載せない。 */
	public static List<Entry> top(int n) {
		load();
		List<Entry> all = new ArrayList<>();
		for (Entry e : MAP.values()) if (e.trophies > 0) all.add(e);
		all.sort((a, b) -> a.trophies != b.trophies ? Integer.compare(b.trophies, a.trophies) : Integer.compare(b.wins, a.wins));
		return all.size() > n ? new ArrayList<>(all.subList(0, n)) : all;
	}
}
