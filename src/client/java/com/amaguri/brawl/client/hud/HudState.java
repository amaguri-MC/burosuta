package com.amaguri.brawl.client.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amaguri.brawl.api.Net;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * サーバーから届いた表示用の状態（クライアントのメインスレッドだけで読み書きする）。
 * payload の受信は U（HudClient）だけが登録する。V などは {@link #hud} {@link #match} {@link #lobby} を読むだけ。
 */
public final class HudState {
	private HudState() {}

	/** クライアント tick の通し番号（START_CLIENT_TICK で +1）。 */
	public static long tick = 0;

	/** 自分のステータス（試合外 null）。ロビーの練習中も届く。 */
	public static Net.HudPayload hud = null;
	/** 試合の状況（試合外 null）。 */
	public static Net.MatchPayload match = null;
	/** ロビーの状況（ロビー外 null）。 */
	public static Net.LobbyPayload lobby = null;

	static long hudTick = -1000, matchTick = -1000, lobbyTick = -1000;
	static Net.BarsPayload bars = null;
	/** パワーキューブ箱のHP（試合中）。 */
	public static Net.BoxesPayload boxes = null;
	static long barsTick = -1000;
	static Set<Integer> hidden = Set.of();
	static long hiddenTick = -1000;
	static Set<Integer> barIds = Set.of();
	/** 状態異常: 実体ID → {flags, frost}。flags: bit0=スタン bit1=凍結 bit2=減速 bit3=氷の床の上。frost: 凍結ゲージ 0〜100。 */
	static Map<Integer, int[]> status = Map.of();
	static long statusTick = -1000;

	// ---- 技名のポップアップ ----
	static String skillText = null;
	static boolean skillSuper = false;
	static long skillStart = -1000, skillLast = -1000;

	/** 技名を出す（同じ技が続くあいだは出しっぱなしにして延長）。 */
	static void showSkill(String text, boolean sup) {
		if (!text.equals(skillText) || tick > skillLast + 20) skillStart = tick;
		skillText = text; skillSuper = sup; skillLast = tick;
	}

	// ---- キルログ ----
	record Kill(String killer, String killerBrawler, String victim, String victimBrawler, long tick) {}
	static final List<Kill> kills = new ArrayList<>();
	static final int KILL_LIFE = 100; // 5秒

	// ---- ダメージ数字 ----
	static final class Pop {
		int id; double x, y, z; int amount; boolean self; long start; float dx;
	}
	static final List<Pop> pops = new ArrayList<>();
	private static final Map<Integer, Integer> lastHp = new HashMap<>();
	static final int POP_LIFE = 28;

	// ---- 演出 ----
	static long hitFlashTick = -1000;       // 自分が被弾した tick
	static int prevMyHp = -1;
	static int prevPhase = 0;
	static int lastCountdown = -1;
	static long countdownStart = -1000;     // カウントダウンの数字が変わった tick
	static long fightStart = -1000;         // 「ファイト！」を出し始めた tick
	static int rankShown = 0;
	static long rankStart = -1000;          // 順位が確定した tick
	static long lobbyCountStart = -1000;
	static int lobbyLastCount = -2;

	// ---- 受信の処理 ----

	static void onHud(Net.HudPayload p) {
		if (p.brawler().isEmpty()) { // 試合外
			hud = null; match = null; bars = null; hidden = Set.of(); barIds = Set.of(); status = Map.of();
			prevMyHp = -1; prevPhase = 0; rankShown = 0;
			return;
		}
		if (prevMyHp >= 0 && p.hp() < prevMyHp && p.alive()) hitFlashTick = tick;
		prevMyHp = p.hp();
		hud = p; hudTick = tick;
	}

	static void onMatch(Net.MatchPayload p) {
		if (p.phase() == 1 && p.countdown() != lastCountdown) { lastCountdown = p.countdown(); countdownStart = tick; }
		if (p.phase() != 1) lastCountdown = -1;
		if (prevPhase == 1 && p.phase() == 2) fightStart = tick;
		prevPhase = p.phase();
		if (p.myRank() > 0 && rankShown != p.myRank()) { rankShown = p.myRank(); rankStart = tick; }
		if (p.myRank() == 0) rankShown = 0;
		if (p.phase() == 0) { match = null; return; }
		match = p; matchTick = tick;
	}

	static void onLobby(Net.LobbyPayload p) {
		if (!p.inLobby()) { lobby = null; return; }
		if (p.countdown() != lobbyLastCount) { lobbyLastCount = p.countdown(); lobbyCountStart = tick; }
		lobby = p; lobbyTick = tick;
	}

	static void onKill(Net.KillFeedPayload p) {
		kills.add(new Kill(p.killer(), p.killerBrawler(), p.victim(), p.victimBrawler(), tick));
		while (kills.size() > 8) kills.remove(0);
	}

	/** BarsPayload の HP 減少を差分で拾い、その実体の頭上にダメージ数字を出す。 */
	static void onBars(Net.BarsPayload p) {
		bars = p; barsTick = tick;
		Set<Integer> ids = new HashSet<>();
		Minecraft mc = Minecraft.getInstance();
		int n = Math.min(p.ids().length, Math.min(p.hp().length, p.team().length));
		Map<Integer, Integer> now = new HashMap<>();
		for (int i = 0; i < p.ids().length; i++) ids.add(p.ids()[i]);
		for (int i = 0; i < n; i++) {
			int id = p.ids()[i], hp = p.hp()[i];
			now.put(id, hp);
			Integer prev = lastHp.get(id);
			if (prev == null || hp >= prev || mc.level == null) continue;
			Entity e = mc.level.getEntity(id);
			if (e == null) continue;
			Pop pop = new Pop();
			pop.id = id; pop.amount = prev - hp; pop.self = p.team()[i] == 0; pop.start = tick;
			pop.x = e.getX(); pop.y = e.getY() + e.getBbHeight() + 0.4; pop.z = e.getZ();
			pop.dx = (float) ((Math.random() - 0.5) * 18.0);
			pops.add(pop);
			if (pops.size() > 24) pops.remove(0);
		}
		lastHp.clear();
		lastHp.putAll(now);
		barIds = ids;
	}

	static void onHidden(Net.HiddenPayload p) {
		Set<Integer> s = new HashSet<>();
		for (int id : p.entityIds()) s.add(id);
		hidden = s; hiddenTick = tick;
	}

	static void onStatus(Net.StatusPayload p) {
		Map<Integer, int[]> m = new HashMap<>();
		int n = Math.min(p.ids().length, Math.min(p.flags().length, p.frost().length));
		for (int i = 0; i < n; i++) m.put(p.ids()[i], new int[] { p.flags()[i], p.frost()[i] });
		status = m; statusTick = tick;
	}

	/** 毎tick: 古いものを捨てる。 */
	static void tickOnce() {
		tick++;
		kills.removeIf(k -> tick - k.tick > KILL_LIFE);
		pops.removeIf(p -> tick - p.start > POP_LIFE);
	}

	public static void reset() {
		barIds = Set.of(); skillText = null;
		hud = null; match = null; bars = null; hidden = Set.of(); lobby = null; status = Map.of();
		statusTick = -1000; hudTick = matchTick = barsTick = hiddenTick = lobbyTick = -1000;
		kills.clear(); pops.clear(); lastHp.clear();
		prevMyHp = -1; prevPhase = 0; lastCountdown = -1; rankShown = 0; lobbyLastCount = -2;
		hitFlashTick = fightStart = countdownStart = rankStart = -1000;
	}

	// ---- 問い合わせ ----

	/** HudPayload が最近届いている（試合中またはロビーの練習中）。 */
	public static boolean active() {
		Net.HudPayload h = hud;
		return h != null && !h.brawler().isEmpty() && tick - hudTick <= 20;
	}

	/** ロビーにいる（LobbyPayload.inLobby が true で、最近届いている）。 */
	public static boolean inLobby() {
		Net.LobbyPayload l = lobby;
		return l != null && l.inLobby() && tick - lobbyTick <= 100;
	}

	/** 試合中（ロビーではない）。試合HUDを出す条件。 */
	public static boolean inMatch() {
		return active() && !inLobby();
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

	/** 試合中に自前の頭上表示へ置き換える実体か（バニラの名前タグを消す対象）。 */
	public static boolean replacesNameTag(Entity e) {
		return active() && (e instanceof net.minecraft.world.entity.player.Player || barIds.contains(e.getId()));
	}

	/** バニラの攻撃・使用・ブロック破壊を止めるか。 */
	public static boolean blockVanillaInput() {
		return alive();
	}

	// ---- 状態異常（StatusPayload） ----

	public static final int ST_STUN = 1, ST_FREEZE = 2, ST_SLOW = 4, ST_ICE_FLOOR = 8;

	/** その実体の状態異常ビット。届いていない・古い（1秒以上）なら 0。 */
	public static int statusFlags(int entityId) {
		if (tick - statusTick > 20) return 0;
		int[] v = status.get(entityId);
		return v == null ? 0 : v[0];
	}

	/** その実体の凍結ゲージ 0〜100。 */
	public static int frost(int entityId) {
		if (tick - statusTick > 20) return 0;
		int[] v = status.get(entityId);
		return v == null ? 0 : v[1];
	}

	/** 状態異常のある実体ID（描画用）。 */
	public static Set<Integer> statusIds() {
		return tick - statusTick > 20 ? Set.of() : status.keySet();
	}

	/** 描画しない実体か（茂みに隠れている）。 */
	public static boolean isHidden(Entity e) {
		if (!active() || hidden.isEmpty() || tick - hiddenTick > 20) return false;
		return hidden.contains(e.getId());
	}
}
