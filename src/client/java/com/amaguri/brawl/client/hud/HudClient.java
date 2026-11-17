package com.amaguri.brawl.client.hud;

import java.util.HashSet;
import java.util.Set;

import com.amaguri.brawl.api.Net;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** W6 hud の入口。受信・HUD・入力・キー登録。 */
public final class HudClient {
	private HudClient() {}

	private static final int GLFW_KEY_R = 82;
	private static KeyMapping superKey;
	private static long lastAttackSend = -100;
	private static boolean pendingClick = false;

	public static void init() {
		// 受信
		ClientPlayNetworking.registerGlobalReceiver(Net.HudPayload.TYPE, (p, ctx) -> {
			if (p.brawler().isEmpty()) { HudState.reset(); return; }
			HudState.hud = p; HudState.hudTick = HudState.tick;
		});
		ClientPlayNetworking.registerGlobalReceiver(Net.MatchPayload.TYPE, (p, ctx) -> {
			HudState.match = p; HudState.matchTick = HudState.tick;
		});
		ClientPlayNetworking.registerGlobalReceiver(Net.HiddenPayload.TYPE, (p, ctx) -> {
			Set<Integer> s = new HashSet<>();
			for (int id : p.entityIds()) s.add(id);
			HudState.hidden = s; HudState.hiddenTick = HudState.tick;
		});
		ClientPlayNetworking.registerGlobalReceiver(Net.BarsPayload.TYPE, (p, ctx) -> {
			HudState.bars = p; HudState.barsTick = HudState.tick;
			Set<Integer> ids = new HashSet<>();
			for (int id : p.ids()) ids.add(id);
			HudState.barIds = ids;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> HudState.reset());

		// キー（キー設定に「必殺技」）
		KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("brawl", "main"));
		superKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.brawl.super", GLFW_KEY_R, cat));

		ClientTickEvents.START_CLIENT_TICK.register(HudClient::tick);

		// 描画: バニラのハート・空腹・経験値などを試合中だけ隠し、自前のHUDを最後に重ねる
		for (Identifier id : new Identifier[] { VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR,
				VanillaHudElements.ARMOR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
				VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.HELD_ITEM_TOOLTIP }) {
			HudElementRegistry.replaceElement(id, orig -> (HudElement) (g, dt) -> {
				if (!HudState.active()) orig.extractRenderState(g, dt);
			});
		}
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("brawl", "hud"), HudRenderer::render);
	}

	private static void tick(Minecraft mc) {
		HudState.tick++;
		if (mc.player == null || mc.level == null) { pendingClick = false; return; }
		if (!HudState.alive() || mc.gui.screen() != null) { pendingClick = false; return; }
		int phase = HudState.phase();
		if (phase == 1 || phase == 3) { // カウントダウン中・終了表示中は撃てない（押下は捨てる）
			while (mc.options.keyAttack.consumeClick()) { }
			while (mc.options.keyUse.consumeClick()) { }
			while (superKey.consumeClick()) { }
			pendingClick = false;
			return;
		}
		float yaw = mc.player.getYRot();

		boolean click = false;
		while (mc.options.keyAttack.consumeClick()) click = true;
		while (mc.options.keyUse.consumeClick()) click = true;
		pendingClick |= click;
		boolean held = mc.options.keyAttack.isDown() || mc.options.keyUse.isDown();
		if ((held || pendingClick) && HudState.tick - lastAttackSend >= 4) {
			ClientPlayNetworking.send(new Net.AttackPayload(yaw));
			skill(false);
			lastAttackSend = HudState.tick;
			pendingClick = false;
		}

		boolean sup = false;
		while (superKey.consumeClick()) sup = true;
		if (sup) { ClientPlayNetworking.send(new Net.SuperPayload(yaw)); skill(true); }
	}

	private static final java.util.Map<String, String[]> SKILLS = java.util.Map.of(
			"rusty", new String[] { "スクラップ散弾", "鉱山ダイナマイト弾" },
			"kaden", new String[] { "ツインレイ", "レイストーム" },
			"gouen", new String[] { "岩石つぶて", "ゴーレムタックル" },
			"mimosa", new String[] { "打ち上げ花火", "大輪スターマイン" },
			"takomaru", new String[] { "張り手連打", "ボディプレス" });

	private static void skill(boolean sup) {
		Net.HudPayload h = HudState.hud;
		if (h == null) return;
		String[] n = SKILLS.get(h.brawler());
		if (n != null) HudState.showSkill(n[sup ? 1 : 0], sup);
	}

	/** 必殺技キーの表示名（HUDのヒント用）。 */
	static String superKeyLabel() {
		return superKey == null ? "R" : superKey.getTranslatedKeyMessage().getString();
	}
}
