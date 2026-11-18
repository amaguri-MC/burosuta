package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.amaguri.brawl.client.view.TopDown;

import net.minecraft.client.Minecraft;

/** 見下ろし中（ロビー含む）はバニラの攻撃・使用・破壊を起こさない。攻撃は TopDownClient が AttackPayload で送る。 */
@Mixin(Minecraft.class)
public abstract class ViewMinecraftMixin {
	@Inject(method = "startAttack()Z", at = @At("HEAD"), cancellable = true)
	private void brawl$noStartAttack(CallbackInfoReturnable<Boolean> cir) {
		if (TopDown.active()) cir.setReturnValue(false);
	}

	@Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
	private void brawl$noContinueAttack(boolean down, CallbackInfo ci) {
		if (TopDown.active()) ci.cancel();
	}

	@Inject(method = "startUseItem()V", at = @At("HEAD"), cancellable = true)
	private void brawl$noUseItem(CallbackInfo ci) {
		if (TopDown.active()) ci.cancel();
	}
}
