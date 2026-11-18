package com.amaguri.brawl.client.view;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.api.Tiles;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.client.hud.HudState;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** V view の入口。BrawlClient.onInitializeClient から TopDownClient.init() を呼ぶ。 */
public final class TopDownClient {
	private TopDownClient() {}

	private static final int GLFW_KEY_V = 86;
	private static KeyMapping toggleKey;
	private static boolean wasActive = false;
	private static long tick = 0;
	private static long lastAttackSend = -100;
	private static boolean pendingClick = false;

	public static void init() {
		KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("brawl", "view"));
		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.brawl.topdown", GLFW_KEY_V, cat));
		ClientTickEvents.START_CLIENT_TICK.register(TopDownClient::tick);
	}

	private static void tick(Minecraft mc) {
		tick++;
		while (toggleKey.consumeClick()) TopDown.enabled = !TopDown.enabled;
		boolean act = TopDown.active();
		// カーソル: 有効中は掴みを外す。無効に戻ったら掴み直す
		if (act) {
			if (mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
		} else if (wasActive && mc.gui.screen() == null && mc.player != null && !mc.mouseHandler.isMouseGrabbed()) {
			mc.mouseHandler.grabMouse();
		}
		wasActive = act;
		if (!act) { pendingClick = false; return; }

		float yaw = TopDown.aimYaw();
		int phase = HudState.phase();
		if (phase == 1 || phase == 3) { // カウントダウン中・終了表示中は撃てない
			while (mc.options.keyAttack.consumeClick()) { }
			while (mc.options.keyUse.consumeClick()) { }
			pendingClick = false;
			return;
		}
		boolean click = false;
		while (mc.options.keyAttack.consumeClick()) click = true;
		while (mc.options.keyUse.consumeClick()) click = true;
		pendingClick |= click;
		boolean held = mc.options.keyAttack.isDown() || mc.options.keyUse.isDown();
		if ((held || pendingClick) && tick - lastAttackSend >= 4) {
			ClientPlayNetworking.send(new Net.AttackPayload(yaw));
			lastAttackSend = tick;
			pendingClick = false;
		}
		if (tick % 2 == 0) guide(mc, yaw);
	}

	/** 照準ガイド: 射程ぶんの帯（幅1ブロック）の両縁をパーティクルの点線で描く。 */
	private static void guide(Minecraft mc, float yaw) {
		String id = null;
		if (HudState.hud != null && !HudState.hud.brawler().isEmpty()) id = HudState.hud.brawler();
		else if (HudState.lobby != null) id = HudState.lobby.selected();
		BrawlerDef def = Brawlers.get(id);
		if (def == null || mc.level == null || mc.player == null) return;
		double range = def.attackRangeTiles() * Tiles.BLOCKS_PER_TILE;
		double r = Math.toRadians(yaw);
		double dx = -Math.sin(r), dz = Math.cos(r);
		double px = -dz, pz = dx; // 横方向
		Vec3 o = mc.player.position();
		for (double d = 1.0; d <= range; d += 1.0) {
			for (int side = -1; side <= 1; side += 2) {
				double x = o.x + dx * d + px * 0.5 * side, z = o.z + dz * d + pz * 0.5 * side;
				mc.level.addParticle(ParticleTypes.END_ROD, x, o.y + 0.1, z, 0, 0, 0);
			}
		}
	}
}
