package com.amaguri.brawl.mode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.api.Tiles;
import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.arena.ArenaMap;
import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;
import com.amaguri.brawl.core.MatchHooks;
import com.amaguri.brawl.lobby.Lobby;
import com.amaguri.brawl.lobby.Stats;

/** バトルロイヤルの進行（カウントダウン→試合→勝利表示→ロビー帰還）。状態は {@link #match} 1つだけ。 */
public final class Showdown {
	private Showdown() {}

	private static final Logger LOG = LoggerFactory.getLogger("brawl-mode");

	static final int TOTAL = 10;
	static final int COUNTDOWN_TICKS = 60;      // 3秒
	static final int ENDING_TICKS = 200;        // 10秒

	private enum Phase { COUNTDOWN, PLAYING, ENDING }

	/** 参加者1人ぶん（プレイヤーもボットも）。 */
	static final class Entrant {
		UUID id;                     // ボットは spawn 後に実体の UUID へ付け直す
		final String name;
		final String brawlerId;
		final boolean bot;
		LivingEntity botEntity;      // ボットのみ
		Vec3 spawn;
		float yaw;
		int rank;                    // 0=未確定
		boolean eliminated;
		// プレイヤーをロビーへ戻す用
		boolean prepared;
		boolean restored;

		Entrant(UUID id, String name, String brawlerId, boolean bot) {
			this.id = id;
			this.name = name;
			this.brawlerId = brawlerId;
			this.bot = bot;
		}
	}

	private record Elim(Entrant victim, Entrant killer, int rank, int cubes) {}

	private static final class Match {
		final ServerLevel level;
		final ArenaLayout layout;
		final AABB arena;
		final List<Entrant> entrants = new ArrayList<>();
		final Map<UUID, Entrant> byId = new LinkedHashMap<>();
		final Set<UUID> alive = new LinkedHashSet<>();
		final ArrayDeque<Elim> pending = new ArrayDeque<>();
		Phase phase = Phase.COUNTDOWN;
		int phaseTicks;
		int playTicks;
		Entrant winner;
		boolean finished;
		/** 試合中に入ってきた（または人数オーバーだった）プレイヤー。観戦して、終了後ロビーへ。 */
		final Set<UUID> watchers = new HashSet<>();

		Match(ServerLevel level, ArenaLayout layout) {
			this.level = level;
			this.layout = layout;
			double x0 = layout.origin().getX(), z0 = layout.origin().getZ();
			this.arena = new AABB(x0 - 4, layout.origin().getY() - 64, z0 - 4,
					x0 + Tiles.b(layout.widthTiles()) + 4, layout.origin().getY() + 128, z0 + Tiles.b(layout.depthTiles()) + 4);
		}
	}

	private static Match match;
	private static final Random RNG = new Random();
	private static boolean inited;
	private static boolean stopping;

	// ------------------------------------------------------------------ 初期化

	public static void init() {
		if (inited) return;
		inited = true;
		MatchHooks.onEliminated = Showdown::onEliminated;
		ServerTickEvents.END_SERVER_TICK.register(Showdown::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onDisconnect(server, handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> stopping = false);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			stopping = true;
			stop(server);
		});
		// 試合中の参加者: ブロックの破壊・設置・使用、アイテムの使用は不可
		PlayerBlockBreakEvents.BEFORE.register((lvl, player, pos, state, be) -> !isMatchPlayer(player));
		UseBlockCallback.EVENT.register((player, lvl, hand, hit) -> isMatchPlayer(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackBlockCallback.EVENT.register((player, lvl, hand, pos, dir) -> isMatchPlayer(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, lvl, hand) -> isMatchPlayer(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		ShowdownCommands.init();
		Lobby.init();                                // ロビー（広場・像・練習場・準備床・掲示板）
	}

	public static boolean running() {
		return match != null;
	}

	/** 試合中（カウントダウン・終了表示を除く）。 */
	public static boolean playing() {
		Match m = match;
		return m != null && m.phase == Phase.PLAYING;
	}

	/** Boxes 用: 試合中のレイアウト。無ければ null。 */
	static ArenaLayout currentLayout() {
		Match m = match;
		return m == null ? null : m.layout;
	}

	private static boolean isMatchPlayer(Player p) {
		Match m = match;
		if (m == null) return false;
		Entrant e = m.byId.get(p.getUUID());
		return e != null && !e.bot && !e.restored;
	}

	// ------------------------------------------------------------------ 開始

	/** 手動開始（/brawl start）: オンラインの全プレイヤーが参加。失敗なら理由（日本語）を返す。成功は null。 */
	static String start(MinecraftServer server, Integer botArg) {
		return start(server, botArg, null);
	}

	/** 試合を始める。participants=参加するプレイヤー（null=オンラインの全員。最大10人）。失敗なら理由（日本語）、成功は null。 */
	public static String start(MinecraftServer server, Integer botArg, List<ServerPlayer> participants) {
		if (match != null) return "すでに試合中です";
		List<ServerPlayer> players = new ArrayList<>(participants != null ? participants : server.getPlayerList().getPlayers());
		if (players.size() > TOTAL) players = new ArrayList<>(players.subList(0, TOTAL));
		int bots = botArg != null ? Math.max(0, botArg) : Math.max(0, TOTAL - players.size());
		bots = Math.min(bots, TOTAL - players.size());
		if (players.size() + bots < 2) return "参加者が2人に足りません（ボットを増やしてください）";

		ServerLevel lvl = server.overworld();
		Match m = null;
		try {
			Lobby.beforeMatch(server);               // カカシを片付ける
			Combat.clearAll(server);                 // 前の状態の取り残しを消す（練習中の参加状態も）
			Boxes.reset();
			ArenaLayout layout = ArenaMap.build(lvl);
			if (layout == null || layout.spawns().size() < players.size() + bots) {
				return "マップのスタート地点が足りません";
			}
			m = new Match(lvl, layout);
			match = m;                               // ここから先の失敗は cleanup が面倒を見る
			Gas.reset(layout);

			for (ServerPlayer p : players) {
				String id = Lobby.selected(p);          // 保存してある選択キャラ（無ければ先頭のキャラ）
				if (Brawlers.get(id) == null) id = randomBrawler();
				add(m, new Entrant(p.getUUID(), p.getName().getString(), id, false));
			}
			for (int i = 1; i <= bots; i++) {
				add(m, new Entrant(UUID.randomUUID(), "ボット" + i, randomBrawler(), true));
			}

			// スタート地点はランダム割り当て
			List<Vec3> spawns = new ArrayList<>(layout.spawns());
			Collections.shuffle(spawns, RNG);
			Vec3 center = layout.center();
			for (int i = 0; i < m.entrants.size(); i++) {
				Entrant e = m.entrants.get(i);
				e.spawn = spawns.get(i);
				e.yaw = (float) Math.toDegrees(Math.atan2(-(center.x - e.spawn.x), center.z - e.spawn.z));
			}
			for (java.util.Iterator<Entrant> it = m.entrants.iterator(); it.hasNext();) {
				Entrant e = it.next();
				if (e.bot) {
					BrawlBot bot = BrawlBot.spawn(lvl, e.spawn, e.brawlerId, e.name); // spawn の中で Combat.join 済み
					m.byId.remove(e.id);
					if (bot == null) { it.remove(); continue; }
					e.botEntity = bot;
					e.id = bot.getUUID();
					m.byId.put(e.id, e);
				} else {
					ServerPlayer p = server.getPlayerList().getPlayer(e.id);
					if (p != null) prepare(m, e, p);
				}
			}
		} catch (RuntimeException ex) {
			LOG.error("試合の開始に失敗", ex);
			cleanup(server);
			return "試合の開始に失敗しました: " + ex;
		}
		for (Entrant e : m.entrants) m.alive.add(e.id);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {   // 参加しない人（人数オーバー）は観戦
			if (!m.byId.containsKey(p.getUUID())) watch(server, p);
		}
		broadcast(m, Msg.t("brawl.mode.start", "バトルロイヤル開始！ 参加 %s人（ボット %s人）", m.entrants.size(), bots).withStyle(ChatFormatting.YELLOW));
		return null;
	}

	private static void add(Match m, Entrant e) {
		m.entrants.add(e);
		m.byId.put(e.id, e);
	}

	private static String randomBrawler() {
		List<BrawlerDef> all = Brawlers.all();
		return all.get(RNG.nextInt(all.size())).id();
	}

	private static void prepare(Match m, Entrant e, ServerPlayer p) {
		e.prepared = true;
		p.closeContainer();
		p.setGameMode(GameType.ADVENTURE);
		Combat.join(p, e.brawlerId);
		Brawlers.giveKit(p);
		p.fallDistance = 0;
		p.teleportTo(m.level, e.spawn.x, e.spawn.y, e.spawn.z, Set.of(), e.yaw, 0f, true);
		BrawlerDef def = Brawlers.get(e.brawlerId);
		Msg.chat(p, Msg.t("brawl.mode.you_are", "あなたのキャラ: %s", def != null ? def.nameJa() : e.brawlerId).withStyle(ChatFormatting.AQUA));
	}

	// ------------------------------------------------------------------ tick

	private static void tick(MinecraftServer server) {
		Match m = match;
		if (m == null) {
			for (net.minecraft.server.level.ServerLevel l : server.getAllLevels()) {
				List<LivingEntity> ps = Combat.participants(l);
				if (!ps.isEmpty()) Cubes.pickupNear(l, ps);
			}
			return;
		}
		try {
			tickMatch(server, m);
		} catch (RuntimeException ex) {
			LOG.error("試合の進行でエラー。試合を中止します", ex);
			cleanup(server);
		}
	}

	private static void tickMatch(MinecraftServer server, Match m) {
		drain(server, m);
		checkMissing(server, m);

		switch (m.phase) {
			case COUNTDOWN -> tickCountdown(server, m);
			case PLAYING -> tickPlaying(server, m);
			case ENDING -> {
				m.phaseTicks++;
				if (m.phaseTicks >= ENDING_TICKS) {
					cleanup(server);
					return;
				}
			}
		}
		if (match != m) return;

		if (m.phase != Phase.ENDING) {
			tickWorld(server, m);
			drain(server, m);
			if (m.alive.size() <= 1) finish(server, m);
		} else {
			tickWorld(server, m);
		}
		if (match == m && server.getTickCount() % 10 == 0) sendMatchPayloads(server, m);
	}

	private static void tickCountdown(MinecraftServer server, Match m) {
		// 動けない: 毎tick スタート地点へ戻す
		for (Entrant e : m.entrants) {
			if (e.eliminated) continue;
			LivingEntity ent = entityOf(server, e);
			if (ent == null) continue;
			if (e.bot) {
				ent.snapTo(e.spawn.x, e.spawn.y, e.spawn.z, e.yaw, 0f);
				ent.setDeltaMovement(Vec3.ZERO);
			} else {
				double dx = ent.getX() - e.spawn.x, dz = ent.getZ() - e.spawn.z;
				if (dx * dx + dz * dz > 0.0025 || Math.abs(ent.getY() - e.spawn.y) > 0.5) {
					((ServerPlayer) ent).connection.teleport(e.spawn.x, e.spawn.y, e.spawn.z, ent.getYRot(), ent.getXRot());
				}
				ent.setDeltaMovement(Vec3.ZERO);
			}
		}
		if (m.phaseTicks % 20 == 0) {
			int sec = 3 - m.phaseTicks / 20;
			for (Entrant e : m.entrants) {
				ServerPlayer p = player(server, e);
				if (p == null) continue;
				Msg.title(p, Component.literal(String.valueOf(sec)).withStyle(ChatFormatting.YELLOW), null, 0, 22, 0);
				m.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 1.0f, 1.0f);
			}
		}
		m.phaseTicks++;
		if (m.phaseTicks >= COUNTDOWN_TICKS) {
			m.phase = Phase.PLAYING;
			m.phaseTicks = 0;
			m.playTicks = 0;
			for (Entrant e : m.entrants) {
				ServerPlayer p = player(server, e);
				if (p == null) continue;
				Msg.title(p, Msg.t("brawl.mode.go", "スタート！").withStyle(ChatFormatting.GREEN), null, 0, 20, 10);
				m.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
			}
		}
	}

	private static void tickPlaying(MinecraftServer server, Match m) {
		m.playTicks++;
		Gas.update(m.playTicks);
		boolean particleTick = Gas.shrinking(m.playTicks) && m.playTicks % 5 == 0;
		boolean damageTick = m.playTicks % 20 == 0;
		double floorY = m.layout.origin().getY();
		for (Entrant e : new ArrayList<>(m.entrants)) {
			if (e.eliminated) continue;
			LivingEntity ent = entityOf(server, e);
			if (ent == null) continue;
			if (particleTick && !e.bot) Gas.edgeParticles(m.level, (ServerPlayer) ent);
			if (ent.getY() < floorY - 20) {
				Combat.damage(ent, null, 100000, false);        // 場外へ落ちた
			} else if (damageTick && Gas.inGas(ent.position())) {
				Combat.damage(ent, null, 1000, false);          // ガスは1秒ごとに1000
			}
		}
	}

	/** 毎tick: キューブの付与、投げ捨てた物の削除、武器の再配布。 */
	private static void tickWorld(MinecraftServer server, Match m) {
		List<LivingEntity> cands = new ArrayList<>();
		if (m.phase == Phase.PLAYING) {
			for (Entrant e : m.entrants) {
				if (e.eliminated) continue;
				LivingEntity ent = entityOf(server, e);
				if (ent != null && ent.isAlive()) {
					BrawlState st = Combat.state(ent);
					if (st != null && st.alive) cands.add(ent);
				}
			}
		}
		Cubes.tick(m.level, m.arena, cands);
		for (Entrant e : m.entrants) {
			if (e.bot || e.eliminated) continue;
			ServerPlayer p = player(server, e);
			if (p != null && !hasWeapon(p, e)) Brawlers.giveKit(p);
		}
	}

	private static boolean hasWeapon(ServerPlayer p, Entrant e) {
		var weapon = Brawlers.weaponItem(e.brawlerId);
		if (weapon == null) return true;
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).getItem() == weapon) return true;
		}
		return false;
	}

	/** 実体が消えている参加者（ボットが消えた・プレイヤーが落ちた）を脱落扱いにする。 */
	private static void checkMissing(MinecraftServer server, Match m) {
		if (m.phase == Phase.ENDING) return;
		for (Entrant e : new ArrayList<>(m.entrants)) {
			if (e.eliminated) continue;
			if (entityOf(server, e) == null) eliminate(m, e, null, 0);
		}
	}

	// ------------------------------------------------------------------ 脱落

	/** MatchHooks から（Combat.damage の中で）呼ばれる。ここでは記録だけして、演出は次の drain で行う。 */
	private static void onEliminated(LivingEntity victim, LivingEntity killer) {
		Match m = match;
		if (victim == null) return;
		if (m == null) {                           // 試合外 = ロビーの練習。カカシは立て直す
			Lobby.onTrainingEliminated(victim);
			return;
		}
		Entrant v = m.byId.get(victim.getUUID());
		if (v == null || v.eliminated) return;
		Entrant k = killer == null ? null : m.byId.get(killer.getUUID());
		BrawlState st = Combat.state(victim);
		eliminate(m, v, k == v ? null : k, st != null ? st.cubes : 0);
	}

	/** 順位=脱落時の生存数。二重に呼ばれても1回しか効かない。 */
	private static void eliminate(Match m, Entrant v, Entrant killer, int cubes) {
		if (v.eliminated) return;
		v.eliminated = true;
		v.rank = Math.max(1, m.alive.size());
		m.alive.remove(v.id);
		m.pending.add(new Elim(v, killer, v.rank, cubes));
	}

	private static void drain(MinecraftServer server, Match m) {
		Elim el;
		while ((el = m.pending.poll()) != null) applyElim(server, m, el);
	}

	private static void applyElim(MinecraftServer server, Match m, Elim el) {
		Entrant v = el.victim();
		LivingEntity ent = entityOf(server, v);
		if (ent != null && el.cubes() > 0) {
			Cubes.drop(m.level, ent.position().add(0, 0.6, 0), el.cubes());
		}
		MutableComponent info;
		if (el.killer() != null) {
			info = Msg.t("brawl.mode.elim_by", "%sは%sに倒された（残り%s人）", v.name, el.killer().name, m.alive.size());
			ServerPlayer kp = player(server, el.killer());
			if (kp != null && !el.killer().eliminated) {
				Msg.actionBar(kp, Msg.t("brawl.mode.you_killed", "%sを倒した！", v.name).withStyle(ChatFormatting.GOLD));
				m.level.playSound(null, kp.getX(), kp.getY(), kp.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
			}
		} else {
			info = Msg.t("brawl.mode.elim", "%sが脱落（残り%s人）", v.name, m.alive.size());
		}
		broadcast(m, info.withStyle(ChatFormatting.GRAY));
		Net.KillFeedPayload feed = new Net.KillFeedPayload(
				el.killer() != null ? el.killer().name : "", el.killer() != null ? el.killer().brawlerId : "",
				v.name, v.brawlerId);
		for (ServerPlayer sp : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(sp, feed);   // 全員へ（観戦・ロビーの人にも）
		if (ent == null) return;
		if (v.bot) {
			m.level.sendParticles(ParticleTypes.POOF, ent.getX(), ent.getY() + 0.8, ent.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
			removeBot(v);
		} else if (ent instanceof ServerPlayer p) {
			p.setGameMode(GameType.SPECTATOR);
			Msg.title(p, Component.literal("#" + v.rank).withStyle(ChatFormatting.RED),
					el.killer() != null ? Msg.t("brawl.mode.killed_by", "%sに倒された", el.killer().name)
							: Msg.t("brawl.mode.out", "脱落"), 5, 50, 15);
			sendMatchPayload(m, v, p);
		}
	}

	private static void removeBot(Entrant e) {
		LivingEntity bot = e.botEntity;
		if (bot == null) return;
		try {
			Combat.leave(bot);
		} catch (RuntimeException ex) {
			LOG.warn("ボットの leave に失敗", ex);
		}
		if (!bot.isRemoved()) bot.discard();
	}

	private static void onDisconnect(MinecraftServer server, ServerPlayer p) {
		Match m = match;
		if (m == null || p == null) return;
		Entrant e = m.byId.get(p.getUUID());
		if (e == null || e.bot || e.restored) return;
		BrawlState st = Combat.state(p);
		if (!e.eliminated) {
			eliminate(m, e, null, st != null ? st.cubes : 0);
			drain(server, m);                       // 持っていたキューブを今の場所に落とす
		}
		try {
			Combat.leave(p);
		} catch (RuntimeException ex) {
			LOG.warn("leave に失敗", ex);
		}
		e.restored = true;                         // 切断する人はロビーへ送らない（入り直したら Lobby/watch が扱う）
		if (match == m && m.phase != Phase.ENDING && m.alive.size() <= 1) finish(server, m);
	}

	// ------------------------------------------------------------------ 勝利・終了

	private static void finish(MinecraftServer server, Match m) {
		if (m.finished) return;
		m.finished = true;
		m.phase = Phase.ENDING;
		m.phaseTicks = 0;
		for (UUID id : m.alive) {
			Entrant w = m.byId.get(id);
			if (w != null) {
				m.winner = w;
				w.rank = 1;
			}
		}
		if (m.winner == null) { // 最後の2人が同時に倒れた: 後に倒れた方(#1)を勝者に
			for (Entrant e : m.entrants) if (e.rank == 1) m.winner = e;
		}
		List<Entrant> order = new ArrayList<>(m.entrants);
		order.sort(Comparator.comparingInt((Entrant e) -> e.rank == 0 ? 99 : e.rank));
		MutableComponent table = Msg.t("brawl.mode.table_head", "===== 結果 =====").withStyle(ChatFormatting.GOLD);
		for (Entrant e : order) {
			BrawlerDef def = Brawlers.get(e.brawlerId);
			table.append(Component.literal("\n#" + e.rank + "  " + e.name + "  (" + (def != null ? def.nameJa() : e.brawlerId) + ")")
					.withStyle(e.rank == 1 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
		}
		// トロフィー（0未満にはしない）と勝利数。ボットは数えない
		Map<UUID, int[]> trophyOf = new java.util.HashMap<>();   // id → {増減, 合計}
		for (Entrant e : order) {
			if (e.bot) continue;
			Stats.Entry st = Stats.get(e.id, e.name);
			int old = st.trophies;
			st.trophies = Math.max(0, old + Stats.delta(e.rank));
			if (e == m.winner) st.wins++;
			trophyOf.put(e.id, new int[] {st.trophies - old, st.trophies});
		}
		Stats.save();
		String[] names = new String[order.size()], brawlers = new String[order.size()];
		int[] ranks = new int[order.size()];
		for (int i = 0; i < order.size(); i++) {
			Entrant e = order.get(i);
			names[i] = e.name;
			brawlers[i] = e.brawlerId;
			ranks[i] = e.rank == 0 ? order.size() : e.rank;
		}
		for (Entrant e : m.entrants) {
			ServerPlayer p = player(server, e);
			if (p == null) continue;
			int[] tr = trophyOf.getOrDefault(e.id, new int[] {0, 0});
			ServerPlayNetworking.send(p, new Net.ResultPayload(e.rank == 0 ? order.size() : e.rank, order.size(),
					tr[0], tr[1], names, brawlers, ranks));
			Msg.chat(p, table);
			if (e == m.winner) {
				Msg.title(p, Msg.t("brawl.mode.win_title", "#1 勝利！").withStyle(ChatFormatting.GOLD), null, 5, 80, 20);
				m.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
			} else {
				Msg.title(p, Msg.t("brawl.mode.rank_title", "#%s", e.rank).withStyle(ChatFormatting.WHITE),
						m.winner != null ? Msg.t("brawl.mode.winner_is", "優勝: %s", m.winner.name) : Msg.t("brawl.mode.no_winner", "勝者なし"),
						5, 80, 20);
			}
			sendMatchPayload(m, e, p);
		}
	}

	/** /brawl stop・サーバー停止・エラー時。即終了して全部元に戻す（何度呼んでも安全）。 */
	static void stop(MinecraftServer server) {
		cleanup(server);
	}

	private static void cleanup(MinecraftServer server) {
		Match m = match;
		match = null;                              // 先に外す（再入しても二重実行しない）
		Gas.clear();
		Boxes.reset();
		if (m == null) {
			Combat.clearAll(server);
			return;
		}
		for (Entrant e : m.entrants) {
			if (e.bot) {
				removeBot(e);
			} else {
				ServerPlayer p = player(server, e);
				if (p != null) restore(server, e, p);
			}
		}
		// 取りこぼしたボット・アイテム（キューブ等）を掃除
		try {
			for (BrawlBot b : m.level.getEntitiesOfClass(BrawlBot.class, m.arena)) {
				if (!b.isRemoved()) b.discard();
			}
			for (var ie : m.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, m.arena)) {
				ie.discard();
			}
		} catch (RuntimeException ex) {
			LOG.warn("掃除に失敗", ex);
		}
		for (UUID id : m.watchers) {                // 観戦していた人もロビーへ
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) Lobby.returnToLobby(server, p);
		}
		Combat.clearAll(server);
		m.pending.clear();
		if (!stopping) Lobby.afterMatch(server);    // カカシを戻す（練習の参加状態はロビーの tick が戻す）
	}

	/**
	 * プレイヤーをロビーへ戻す（アドベンチャー・ロビーのスポーン）。1人につき1回だけ。
	 * 練習中の参加者（Combat.join・武器だけの持ち物）に戻すのは、cleanup の Combat.clearAll のあとにロビーの tick が行う。
	 */
	private static void restore(MinecraftServer server, Entrant e, ServerPlayer p) {
		if (e.restored || !e.prepared) return;
		e.restored = true;
		Lobby.returnToLobby(server, p);
	}

	/** 試合中に入ってきた人・人数オーバーの人: 観戦（スペクテイター）。終了後にロビーへ戻す。 */
	public static void watch(MinecraftServer server, ServerPlayer p) {
		Match m = match;
		if (m == null || p == null) return;
		Entrant e = m.byId.get(p.getUUID());
		if (e != null && !e.restored && !e.bot) return;      // 試合に参加している人は対象外
		m.watchers.add(p.getUUID());
		if (Combat.state(p) != null) Combat.leave(p);
		p.closeContainer();
		p.setGameMode(GameType.SPECTATOR);
		Vec3 c = m.layout.center();
		p.teleportTo(m.level, c.x, c.y + 12, c.z, Set.of(), p.getYRot(), 30f, true);
		Msg.chat(p, Msg.t("brawl.mode.watching", "試合中です。終わるまで観戦します").withStyle(ChatFormatting.GRAY));
	}

	// ------------------------------------------------------------------ 通信・補助

	private static void sendMatchPayloads(MinecraftServer server, Match m) {
		for (Entrant e : m.entrants) {
			ServerPlayer p = player(server, e);
			if (p != null) sendMatchPayload(m, e, p);
		}
	}

	private static void sendMatchPayload(Match m, Entrant e, ServerPlayer p) {
		if (e.restored) return;
		int phase = switch (m.phase) { case COUNTDOWN -> 1; case PLAYING -> 2; case ENDING -> 3; };
		int cd = m.phase == Phase.COUNTDOWN ? (COUNTDOWN_TICKS - m.phaseTicks + 19) / 20 : 0;
		AABB z = Gas.safeZone();
		int toNext = m.phase == Phase.COUNTDOWN ? Gas.secondsToNext(0) : Gas.secondsToNext(m.playTicks);
		ServerPlayNetworking.send(p, new Net.MatchPayload(phase, cd, m.alive.size(), m.entrants.size(),
				z.minX, z.minZ, z.maxX, z.maxZ, toNext, e.rank));
	}

	private static void broadcast(Match m, Component c) {
		MinecraftServer server = m.level.getServer();
		for (Entrant e : m.entrants) {
			ServerPlayer p = player(server, e);
			if (p != null) Msg.chat(p, c);
		}
	}

	private static ServerPlayer player(MinecraftServer server, Entrant e) {
		return e.bot ? null : server.getPlayerList().getPlayer(e.id);
	}

	/** 参加者の実体。ボットは消えていたら null、プレイヤーはオフラインなら null。 */
	private static LivingEntity entityOf(MinecraftServer server, Entrant e) {
		if (e.bot) {
			LivingEntity b = e.botEntity;
			return b == null || b.isRemoved() ? null : b;
		}
		ServerPlayer p = server.getPlayerList().getPlayer(e.id);
		return p == null || p.isRemoved() ? null : p;
	}
}
