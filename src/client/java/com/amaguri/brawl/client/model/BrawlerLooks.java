package com.amaguri.brawl.client.model;

import java.util.HashMap;
import java.util.Map;

/** 実体ID → キャラID（サーバーの LooksPayload の写し）。空=試合なし。 */
public final class BrawlerLooks {
	private BrawlerLooks() {
	}

	private static volatile Map<Integer, String> map = Map.of();

	public static void set(int[] ids, String[] brawlers) {
		Map<Integer, String> m = new HashMap<>();
		int n = Math.min(ids.length, brawlers.length);
		for (int i = 0; i < n; i++) {
			if (!brawlers[i].isEmpty()) {
				m.put(ids[i], brawlers[i]);
			}
		}
		map = m;
	}

	public static void clear() {
		map = Map.of();
	}

	/** その実体のキャラID。載っていなければ null。 */
	public static String brawlerOf(int entityId) {
		return map.get(entityId);
	}

	/** その実体に着せるモデル。キャラが載っていない、またはモデル/テクスチャのファイルが無ければ null（= ふつうの見た目）。 */
	public static BrawlerModel modelOf(int entityId) {
		String id = map.get(entityId);
		return id == null ? null : BrawlerModels.get(id);
	}
}
