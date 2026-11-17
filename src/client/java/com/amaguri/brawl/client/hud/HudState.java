package com.amaguri.brawl.client.hud;

import java.util.Set;

import com.amaguri.brawl.api.Net;

import net.minecraft.world.entity.Entity;

/** サーバーから届いた表示用の状態（クライアントのメインスレッドだけで読み書きする）。 */
public final class HudState {
	private HudState() {}

	/** クライアント tick の通し番号（START_CLIENT_TICK で +1）。 */
	public static long tick = 0;

	public static Net.HudPayload hud = null;
	public static long hudTick = -1000;
	public static Net.MatchPayload match = null;
	public static long matchTick = -1000;
	public static Net.BarsPayload bars = null;
	public static long barsTick = -1000;
	public static Set<Integer> hidden = Set.of();
	public static long hiddenTick = -1000;

	public static void reset() {
		hud = null; match = null; bars = null; hidden = Set.of();
		hudTick = matchTick = barsTick = hiddenTick = -1000;
	}

	/** 試合中（HudPayload の brawler が空でなく、最近届いている）。 */
	public static boolean active() {
		Net.HudPayload h = hud;
		return h != null && !h.brawler().isEmpty() && tick - hudTick <= 20;
	}

	public static boolean alive() {
		Net.HudPayload h = hud;
		return active() && h.alive();
	}

	/** 0=試合なし 1=カウントダウン 2=試合中 3=終了表示 */
	public static int phase() {
		Net.MatchPayload m = match;
		return (m != null && tick - matchTick <= 60) ? m.phase() : 0;
	}

	public static Net.MatchPayload matchOrNull() {
		return (match != null && tick - matchTick <= 60) ? match : null;
	}

	public static Net.BarsPayload barsOrNull() {
		return (bars != null && tick - barsTick <= 10) ? bars : null;
	}

	/** バニラの攻撃・使用・ブロック破壊を止めるか。 */
	public static boolean blockVanillaInput() {
		return alive();
	}

	/** 描画しない実体か（茂みに隠れている）。 */
	public static boolean isHidden(Entity e) {
		if (!active() || hidden.isEmpty() || tick - hiddenTick > 20) return false;
		return hidden.contains(e.getId());
	}
}
