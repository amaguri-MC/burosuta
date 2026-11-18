package com.amaguri.brawl.lobby;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;
import com.amaguri.brawl.core.MatchHooks;
import com.amaguri.brawl.mode.Showdown;

/**
 * ロビー: 広場・像・練習場・準備床・掲示板と、試合との行き来。
 * ロビーにいる（広場の範囲内の）プレイヤーは「練習中の参加者」（Combat.join）で、カカシを撃てる。
 * 準備床に全員が乗ると 5秒後に Showdown 開始。試合中は何もしない（人の出入りは Showdown 側）。
 */
public final class Lobby {
	private Lobby() {}

	private static final Logger LOG = LoggerFactory.getLogger("brawl-lobby");

	private static final int READY_TICKS = 100;      // 5秒
	private static final int PAYLOAD_EVERY = 10;

	private static boolean inited;
	private static boolean built;
	private static long tickCount;

	/** いまロビーにいる（LobbyPayload を送っている）プレイヤー。 */
	private static final Set<UUID> IN_LOBBY = new HashSet<>();
	private static int countdown = -1;               // 残りtick。-1=待機中
	private static int lastSec = -1;
	private static int cooldown;                     // 開始に失敗した直後などの待ち

	// /brawl setlobby で上書きしたスポーン
	private static String customDim;
	private static double cx, cy, cz;
	private static float cyaw;
	private static boolean customSet;
	private static boolean customLoaded;

	// ------------------------------------------------------------------ 初期化

	public static void init() {
		if (inited) return;
		inited = true;
		LobbyBlocks.init();
		MatchHooks.canHit = Lobby::canHit;
		ServerTickEvents.END_SERVER_TICK.register(Lobby::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> build(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			built = false;                           // 実体は保存しない。次の起動で建て直す
			IN_LOBBY.clear();
			countdown = -1;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(server, handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> IN_LOBBY.remove(handler.getPlayer().getUUID()));
		ServerPlayNetworking.registerGlobalReceiver(Net.PickPayload.TYPE, (payload, ctx) -> pick(ctx.player(), payload.brawler()));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (hand != InteractionHand.MAIN_HAND || level.isClientSide()) return InteractionResult.PASS;
			if (!(player instanceof ServerPlayer sp) || !(entity instanceof BrawlBot bot) || !LobbyEntities.isStatue(entity)) {
				return InteractionResult.PASS;
			}
			if (!Showdown.running()) ServerPlayNetworking.send(sp, new Net.OpenSelectPayload(bot.getBrawlerId()));
			return InteractionResult.SUCCESS;
		});
	}

	/** ロビーを建てて、像とカカシを置き直す（実体は保存しない）。起動時と /brawl lobby。何度呼んでも二重にならない。 */
	public static boolean build(MinecraftServer server) {
		if (Showdown.running()) return false;
		try {
			ServerLevel lvl = server.overworld();
			LobbyLayout L = LobbyMap.build(lvl);
			Combat.clearAll(server);                 // カカシ・練習中の状態を作り直す
			LobbyEntities.rebuild(lvl, L);
			built = true;
			countdown = -1;
			return true;
		} catch (RuntimeException ex) {
			LOG.error("ロビーを建てられませんでした", ex);
			return false;
		}
	}

	public static boolean built() {
		return built;
	}

	// ------------------------------------------------------------------ 試合との接続（Showdown から）

	/** 試合を始める直前: カカシを片付ける（Combat.clearAll の前）。 */
	public static void beforeMatch(MinecraftServer server) {
		countdown = -1;
		LobbyEntities.removeDummies();
	}

	/** 試合の後始末が終わったあと: カカシを戻し、準備を待ち直す。練習の参加状態は tick が戻す。 */
	public static void afterMatch(MinecraftServer server) {
		countdown = -1;
		lastSec = -1;
		cooldown = 40;
		if (!built) return;
		try {
			LobbyEntities.maintain(server.overworld(), LobbyMap.layout());
			LobbyEntities.refreshBoard();
		} catch (RuntimeException ex) {
			LOG.warn("試合後のロビーの立て直しに失敗", ex);
		}
	}

	/** 練習中の参加者・カカシが倒れたとき（試合外）。カカシは全回復して立て直す。 */
	public static void onTrainingEliminated(LivingEntity victim) {
		if (victim == null) return;
		if (LobbyEntities.isDummy(victim) || victim instanceof ServerPlayer) LobbyEntities.reviveDummy(victim);
	}

	/** プレイヤー→カカシだけ通す（ロビー）。試合中は今までどおり全部通す。 */
	private static boolean canHit(LivingEntity attacker, LivingEntity victim) {
		if (Showdown.running()) return true;
		return attacker instanceof ServerPlayer && LobbyEntities.isDummy(victim);
	}

	/** プレイヤーをロビーのスポーンへ送り、アドベンチャーにする。練習への参加は tick がやる。 */
	public static void returnToLobby(MinecraftServer server, ServerPlayer p) {
		if (p == null) return;
		p.closeContainer();
		p.setGameMode(GameType.ADVENTURE);
		p.setHealth(p.getMaxHealth());
		p.clearFire();
		p.fallDistance = 0;
		SpawnPoint sp = spawn(server);
		p.teleportTo(sp.level, sp.pos.x, sp.pos.y, sp.pos.z, Set.of(), sp.yaw, 0f, true);
		ServerPlayNetworking.send(p, new Net.MatchPayload(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
	}

	private static void onJoin(MinecraftServer server, ServerPlayer p) {
		Stats.get(p.getUUID(), p.getName().getString());
		if (Showdown.running()) {
			Showdown.watch(server, p);
		} else if (built) {
			returnToLobby(server, p);
		}
	}

	// ------------------------------------------------------------------ キャラ選択

	/** 選んでいるキャラのID（未選択・不正なら最初のキャラにして保存）。 */
	public static String selected(ServerPlayer p) {
		Stats.Entry e = Stats.get(p.getUUID(), p.getName().getString());
		if (Brawlers.get(e.selected) == null) {
			List<BrawlerDef> all = Brawlers.all();
			e.selected = all.isEmpty() ? "" : all.get(0).id();
			Stats.save();
		}
		return e.selected;
	}

	/** 選んだキャラを保存し、練習のキャラも切り替える。試合中は何もしない（false）。 */
	public static boolean pick(ServerPlayer p, String id) {
		if (p == null || Showdown.running()) return false;
		BrawlerDef def = Brawlers.get(id);
		if (def == null) return false;
		Stats.Entry e = Stats.get(p.getUUID(), p.getName().getString());
		e.selected = def.id();
		Stats.save();
		if (IN_LOBBY.contains(p.getUUID())) upkeep(p);
		p.sendSystemMessage(Component.translatableWithFallback("brawl.lobby.picked", "%sを選んだ！", def.nameJa())
				.withStyle(ChatFormatting.AQUA), false);
		return true;
	}

	// ------------------------------------------------------------------ スポーン

	private record SpawnPoint(ServerLevel level, Vec3 pos, float yaw) {}

	private static SpawnPoint spawn(MinecraftServer server) {
		loadCustom();
		if (customSet) {
			Identifier id = Identifier.tryParse(customDim);
			ServerLevel l = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
			if (l != null) return new SpawnPoint(l, new Vec3(cx, cy, cz), cyaw);
		}
		LobbyLayout L = LobbyMap.layout();
		return new SpawnPoint(server.overworld(), L.spawn(), L.spawnYaw());
	}

	private static Path spawnFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("brawl_lobby.txt");
	}

	private static void loadCustom() {
		if (customLoaded) return;
		customLoaded = true;
		try {
			Path f = spawnFile();
			if (!Files.exists(f)) return;
			String[] a = Files.readString(f, StandardCharsets.UTF_8).trim().split("\\s+");
			if (a.length < 5) return;
			customDim = a[0];
			cx = Double.parseDouble(a[1]);
			cy = Double.parseDouble(a[2]);
			cz = Double.parseDouble(a[3]);
			cyaw = Float.parseFloat(a[4]);
			customSet = true;
		} catch (IOException | RuntimeException e) {
			customSet = false;
		}
	}

	/** /brawl setlobby: このロビーのスポーンを上書きする。 */
	public static void setSpawn(ServerPlayer p) {
		loadCustom();
		customDim = p.level().dimension().identifier().toString();
		cx = p.getX();
		cy = p.getY();
		cz = p.getZ();
		cyaw = p.getYRot();
		customSet = true;
		try {
			Path f = spawnFile();
			Files.createDirectories(f.getParent());
			Files.writeString(f, customDim + " " + cx + " " + cy + " " + cz + " " + cyaw + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			// 保存に失敗しても今回の起動中は有効
		}
	}

	// ------------------------------------------------------------------ tick

	/** ロビーにいるか（広場の範囲、または setlobby したスポーンの近く）。観戦モードの人は含めない。 */
	private static boolean inArea(MinecraftServer server, ServerPlayer p) {
		if (p.isRemoved() || p.isSpectator()) return false;
		if (p.level() == server.overworld() && LobbyMap.layout().bounds().contains(p.position())) return true;
		loadCustom();
		if (customSet) {
			SpawnPoint sp = spawn(server);
			if (p.level() == sp.level) {
				double dx = p.getX() - sp.pos.x, dz = p.getZ() - sp.pos.z, dy = p.getY() - sp.pos.y;
				return dx * dx + dz * dz <= 64 && Math.abs(dy) <= 8;
			}
		}
		return false;
	}

	private static void tick(MinecraftServer server) {
		if (!built) return;
		tickCount++;
		boolean running = Showdown.running();
		List<ServerPlayer> inArea = new ArrayList<>();
		if (!running) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) if (inArea(server, p)) inArea.add(p);
		}
		Set<UUID> now = new HashSet<>();
		for (ServerPlayer p : inArea) now.add(p.getUUID());

		// ロビーを出た人: 練習をやめ、inLobby=false を1回だけ送る
		for (UUID id : new ArrayList<>(IN_LOBBY)) {
			if (now.contains(id)) continue;
			IN_LOBBY.remove(id);
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) continue;
			if (!running && Combat.state(p) != null) Combat.leave(p);
			sendLobby(p, false, 0, 0, -1, false);
		}
		if (running) {
			countdown = -1;
			return;
		}
		// 入った人
		for (ServerPlayer p : inArea) {
			if (IN_LOBBY.add(p.getUUID())) {
				if (p.gameMode() != GameType.ADVENTURE) p.setGameMode(GameType.ADVENTURE);
				upkeep(p);
			}
		}
		if (tickCount % 5 == 0) for (ServerPlayer p : inArea) upkeep(p);

		tickReady(server, inArea);

		ServerLevel lvl = server.overworld();
		if (tickCount % 10 == 0) LobbyEntities.maintain(lvl, LobbyMap.layout());
		if (tickCount % 20 == 0) {
			for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, LobbyMap.layout().bounds())) ie.discard(); // 投げ捨てた物
		}
		if (tickCount % 200 == 0) LobbyEntities.refreshBoard();
	}

	/** 練習中の参加者にする（選択キャラで Combat.join し、武器だけ持たせる）。 */
	private static void upkeep(ServerPlayer p) {
		String sel = selected(p);
		BrawlState st = Combat.state(p);
		if (st == null || !st.brawlerId.equals(sel)) {
			if (st != null) Combat.leave(p);
			Combat.join(p, sel);
			Brawlers.giveKit(p);
		} else if (!hasWeapon(p, sel)) {
			Brawlers.giveKit(p);
		}
	}

	private static boolean hasWeapon(ServerPlayer p, String id) {
		var weapon = Brawlers.weaponItem(id);
		if (weapon == null) return true;
		var inv = p.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).getItem() == weapon) return true;
		}
		return false;
	}

	private static boolean onPad(MinecraftServer server, ServerPlayer p) {
		return p.level() == server.overworld() && LobbyMap.layout().pad().contains(p.position());
	}

	/** 準備床: 全員（1人以上）が乗ったら5秒で開始。誰かが降りたら中止。 */
	private static void tickReady(MinecraftServer server, List<ServerPlayer> lobby) {
		int total = lobby.size();
		int ready = 0;
		for (ServerPlayer p : lobby) if (onPad(server, p)) ready++;
		if (cooldown > 0) cooldown--;

		if (total > 0 && ready == total && cooldown == 0) {
			if (countdown < 0) {
				countdown = READY_TICKS;
				lastSec = -1;
			} else {
				countdown--;
			}
			int sec = (countdown + 19) / 20;
			if (countdown > 0 && sec != lastSec) {
				lastSec = sec;
				for (ServerPlayer p : lobby) {
					title(p, Component.literal(String.valueOf(sec)).withStyle(ChatFormatting.YELLOW),
							Component.translatableWithFallback("brawl.lobby.starting", "まもなく開始！"));
					p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 1.0f, 1.2f);
				}
			}
			if (countdown <= 0) {
				countdown = -1;
				String err = Showdown.start(server, null, new ArrayList<>(lobby));
				if (err != null) {
					cooldown = 100;
					for (ServerPlayer p : lobby) {
						p.sendSystemMessage(Component.literal(err).withStyle(ChatFormatting.RED), false);
					}
				}
				return;
			}
		} else if (countdown >= 0) {
			countdown = -1;
			lastSec = -1;
			for (ServerPlayer p : lobby) {
				p.sendSystemMessage(Component.translatableWithFallback("brawl.lobby.cancel", "準備を中止しました").withStyle(ChatFormatting.GRAY), true);
			}
		}

		if (tickCount % PAYLOAD_EVERY == 0) {
			int cd = countdown < 0 ? -1 : (countdown + 19) / 20;
			for (ServerPlayer p : lobby) sendLobby(p, true, ready, total, cd, onPad(server, p));
		}
	}

	private static void sendLobby(ServerPlayer p, boolean in, int ready, int total, int cd, boolean meReady) {
		Stats.Entry e = Stats.get(p.getUUID());
		ServerPlayNetworking.send(p, new Net.LobbyPayload(in, ready, total, cd, e.trophies, e.wins,
				in ? selected(p) : e.selected, meReady));
	}

	private static void title(ServerPlayer p, Component title, Component sub) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 22, 0));
		p.connection.send(new ClientboundSetSubtitleTextPacket(sub == null ? Component.empty() : sub));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}
}
