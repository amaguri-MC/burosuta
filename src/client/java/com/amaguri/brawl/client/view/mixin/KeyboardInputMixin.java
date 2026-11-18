package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.amaguri.brawl.client.view.TopDown;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;

/** 見下ろし中は WASD を画面基準（W=北）に回して移動ベクトルへ。 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	// UNVERIFIED: moveVector は x=左向きの入力, y=前向きの入力（バニラの Vec2(leftImpulse, forwardImpulse)）
	@Inject(method = "tick()V", at = @At("TAIL"))
	private void brawl$screenRelative(CallbackInfo ci) {
		if (!TopDown.active()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Vec2 v = this.moveVector;
		if (v.x == 0f && v.y == 0f) return;
		float[] r = TopDown.rotateInput(v.x, v.y, mc.player.getYRot());
		this.moveVector = new Vec2(r[0], r[1]);
	}
}
