package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.amaguri.brawl.client.view.TopDown;

import net.minecraft.client.MouseHandler;

/** 見下ろし中はマウスを掴み直さず、マウス移動でも視点を回さない。 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "grabMouse()V", at = @At("HEAD"), cancellable = true)
	private void brawl$noGrab(CallbackInfo ci) {
		if (TopDown.active()) ci.cancel();
	}

	// UNVERIFIED: private turnPlayer(D)V（apidump で実在確認済み）
	@Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
	private void brawl$noTurn(double d, CallbackInfo ci) {
		if (TopDown.active()) ci.cancel();
	}
}
