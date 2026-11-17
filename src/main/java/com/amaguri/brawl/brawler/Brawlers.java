package com.amaguri.brawl.brawler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;

public final class Brawlers {
	private Brawlers() {}

	private static final Map<String, BrawlerDef> DEFS = new LinkedHashMap<>();
	private static final Map<String, Item> WEAPONS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_ATTACK = new HashMap<>();
	private static boolean inited;

	public static void init() {
		if (inited) return;
		inited = true;
		for (BrawlerDef d : BrawlerStats.defs()) {
			DEFS.put(d.id(), d);
			Identifier id = Identifier.fromNamespaceAndPath("brawl", d.id() + "_weapon");
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
			// 見た目だけの Item（use/攻撃/設置の効果なし）。UNVERIFIED: Item.Properties#setId は ref-aibot と同じ使い方
			WEAPONS.put(d.id(), Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(key))));
		}
		Tasks.init();
		// ハンドラはサーバースレッドで呼ばれる（Fabric）。試合外・脱落後は attack/useSuper 側で false
		ServerPlayNetworking.registerGlobalReceiver(Net.AttackPayload.TYPE, (payload, ctx) -> attack(ctx.player(), payload.yaw()));
		ServerPlayNetworking.registerGlobalReceiver(Net.SuperPayload.TYPE, (payload, ctx) -> useSuper(ctx.player(), payload.yaw()));
	}

	public static BrawlerDef get(String id) { return id == null ? null : DEFS.get(id); }

	public static List<BrawlerDef> all() { return List.copyOf(DEFS.values()); }

	/** 試合中で生きている参加者の状態。そうでなければ null。 */
	private static BrawlState activeState(LivingEntity e) {
		BrawlState st = Combat.state(e);
		return st != null && st.alive && !e.isRemoved() ? st : null;
	}

	public static boolean attack(LivingEntity user, float yawDeg) {
		if (user == null || !(user.level() instanceof ServerLevel lvl)) return false;
		if (com.amaguri.brawl.mode.Showdown.running() && !com.amaguri.brawl.mode.Showdown.playing()) return false;
		BrawlState st = activeState(user);
		if (st == null) return false;
		BrawlerDef def = get(st.brawlerId);
		if (def == null) return false;
		int now = lvl.getServer().getTickCount();
		Integer last = LAST_ATTACK.get(user.getUUID());
		if (last != null && now - last < def.attackCooldownTicks()) return false;
		if (!Combat.consumeAmmo(user)) return false;
		LAST_ATTACK.put(user.getUUID(), now);
		Attacks.normal(lvl, user, def.id(), yawDeg);
		return true;
	}

	public static boolean useSuper(LivingEntity user, float yawDeg) {
		if (user == null || !(user.level() instanceof ServerLevel lvl)) return false;
		if (com.amaguri.brawl.mode.Showdown.running() && !com.amaguri.brawl.mode.Showdown.playing()) return false;
		BrawlState st = activeState(user);
		if (st == null) return false;
		BrawlerDef def = get(st.brawlerId);
		if (def == null) return false;
		if (!Combat.consumeSuper(user)) return false;
		Attacks.superMove(lvl, user, def.id(), yawDeg);
		return true;
	}

	/** 持ち物を空にしてスロット1（ホットバー先頭）に武器。UNVERIFIED: 呼ぶ時点で join 済みなら選択中キャラ、未参加なら何も持たせない。 */
	public static void giveKit(ServerPlayer p) {
		p.getInventory().clearContent();
		BrawlState st = Combat.state(p);
		if (st == null) return;
		Item w = weaponItem(st.brawlerId);
		if (w == null) return;
		p.getInventory().setItem(0, new ItemStack(w));
		p.getInventory().setSelectedSlot(0);
	}

	public static Item weaponItem(String brawlerId) { return brawlerId == null ? null : WEAPONS.get(brawlerId); }
}
