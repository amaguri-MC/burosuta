package com.amaguri.brawl.client.hud;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.client.select.SelectScreen;
import com.amaguri.brawl.client.view.TopDown;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** U ui の入口。受信（Hud/Match/Hidden/Bars/KillFeed/Result/Lobby/OpenSelect）・HUD描画・入力・キー登録。 */
public final class HudClient {
	private HudClient() {}

	private static final int GLFW_KEY_R = 82;
	private static KeyMapping superKey;
	private static KeyMapping selectKey;
	private static long lastAttackSend = -100;
	private static boolean pendingClick = false;

	public static void init() {
		// 受信（クライアントのメインスレッドで呼ばれる）
		ClientPlayNetworking.registerGlobalReceiver(Net.HudPayload.TYPE, (p, ctx) -> HudState.onHud(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.MatchPayload.TYPE, (p, ctx) -> HudState.onMatch(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.HiddenPayload.TYPE, (p, ctx) -> HudState.onHidden(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.BarsPayload.TYPE, (p, ctx) -> HudState.onBars(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.StatusPayload.TYPE, (p, ctx) -> HudState.onStatus(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.KillFeedPayload.TYPE, (p, ctx) -> HudState.onKill(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.LobbyPayload.TYPE, (p, ctx) -> HudState.onLobby(p));
		ClientPlayNetworking.registerGlobalReceiver(Net.ResultPayload.TYPE, (p, ctx) -> {
			Minecraft mc = Minecraft.getInstance();
			mc.gui.setScreen(new ResultScreen(p));
		});
		ClientPlayNetworking.registerGlobalReceiver(Net.OpenSelectPayload.TYPE, (p, ctx) -> {
			Minecraft mc = Minecraft.getInstance();
			mc.gui.setScreen(new SelectScreen(p.focus()));
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> HudState.reset());

		// キー（キー設定に「必殺技」「キャラ選択」）
		KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("brawl", "main"));
		superKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.brawl.super", GLFW_KEY_R, cat));
		selectKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.brawl.select", Ui.KEY_B, cat));

		ClientTickEvents.START_CLIENT_TICK.register(HudClient::tick);

		// 描画: バニラのハート・空腹・経験値・ホットバーなどを試合中・ロビーで隠し、自前のHUDを最後に重ねる
		for (Identifier id : new Identifier[] { VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR,
				VanillaHudElements.ARMOR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
				VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.HOTBAR,
				VanillaHudElements.AIR_BAR }) {
			HudElementRegistry.replaceElement(id, orig -> (HudElement) (g, dt) -> {
				if (!HudState.active()) orig.extractRenderState(g, dt);
			});
		}
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("brawl", "hud"), HudRenderer::render);
	}

	private static void tick(Minecraft mc) {
		HudState.tickOnce();
		if (mc.player == null || mc.level == null) { pendingClick = false; return; }

		// B: キャラ選択（試合中は開かない）
		if (mc.gui.screen() == null && !HudState.inMatch()) {
			boolean open = false;
			while (selectKey.consumeClick()) open = true;
			if (open) mc.gui.setScreen(new SelectScreen(HudState.lobby != null ? HudState.lobby.selected() : ""));
		} else {
			while (selectKey.consumeClick()) { }
		}

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

		// 見下ろし視点中のクリック攻撃は V が持つ（R の必殺はこちら）
		if (!TopDown.active()) {
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
		} else {
			pendingClick = false;
		}

		boolean sup = false;
		while (superKey.consumeClick()) sup = true;
		if (sup) { ClientPlayNetworking.send(new Net.SuperPayload(yaw)); skill(true); }
	}

	private static void skill(boolean sup) {
		Net.HudPayload h = HudState.hud;
		if (h == null) return;
		HudState.showSkill(Component.translatable("brawl.skill." + h.brawler() + (sup ? ".super" : ".normal")).getString(), sup);
	}

	/** 必殺技キーの表示名（HUDのヒント用）。 */
	static String superKeyLabel() {
		return Ui.keyLabel(superKey, "R");
	}

	/** キャラ選択キーの表示名。 */
	static String selectKeyLabel() {
		return Ui.keyLabel(selectKey, "B");
	}
}
