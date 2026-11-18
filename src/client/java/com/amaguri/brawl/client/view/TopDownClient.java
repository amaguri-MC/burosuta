package com.amaguri.brawl.client.view;

import com.amaguri.brawl.api.Net;
import com.amaguri.brawl.client.hud.HudState;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
		com.amaguri.brawl.client.world.WorldOverlay.init(); // 毒ガス・射程ガイドの専用描画（G）
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
		boolean inLobby = HudState.lobby != null && HudState.lobby.inLobby();
		while (mc.options.keyAttack.consumeClick()) click = true;
		boolean use = false;
		while (mc.options.keyUse.consumeClick()) use = true;
		if (use && inLobby) interactUnderCursor(mc); // ロビーの右クリック = カーソルの先の像を選ぶ
		else if (use) click = true;
		pendingClick |= click;
		boolean held = mc.options.keyAttack.isDown() || (!inLobby && mc.options.keyUse.isDown());
		if ((held || pendingClick) && tick - lastAttackSend >= 4) {
			ClientPlayNetworking.send(new Net.AttackPayload(yaw, aimDistance(mc)));
			lastAttackSend = tick;
			pendingClick = false;
		}
	}

	/** 見下ろし中の照準点までの水平距離（ブロック）。照準点が無ければ 0。 */
	public static float aimDistance(Minecraft mc) {
		Vec3 a = TopDown.aimPoint();
		if (a == null || mc.player == null) return 0f;
		return (float) Math.hypot(a.x - mc.player.getX(), a.z - mc.player.getZ());
	}

	/** カーソルの光線に当たる一番近い実体へ、バニラの「右クリック」を送る。 */
	private static void interactUnderCursor(Minecraft mc) {
		Vec3 o = TopDown.rayOrigin, d = TopDown.rayDirection;
		if (o == null || d == null || mc.level == null || mc.player == null || mc.gameMode == null) return;
		Vec3 end = o.add(d.normalize().scale(48));
		net.minecraft.world.entity.Entity best = null;
		double bestD = Double.MAX_VALUE;
		for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
			if (e == mc.player || !(e instanceof net.minecraft.world.entity.LivingEntity)) continue;
			var hit = e.getBoundingBox().inflate(0.3).clip(o, end);
			if (hit.isPresent()) {
				double dd = hit.get().distanceToSqr(o);
				if (dd < bestD) { bestD = dd; best = e; }
			}
		}
		if (best != null) {
			mc.gameMode.interact(mc.player, best, new net.minecraft.world.phys.EntityHitResult(best),
					net.minecraft.world.InteractionHand.MAIN_HAND);
		}
	}
}
