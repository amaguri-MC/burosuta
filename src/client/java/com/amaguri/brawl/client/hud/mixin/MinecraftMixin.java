package com.amaguri.brawl.client.hud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.amaguri.brawl.client.hud.HudState;

import net.minecraft.client.Minecraft;

/** 試合中（生存中）はバニラの攻撃・ブロック破壊・使用を止める。攻撃は HudClient が AttackPayload で送る。 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	// UNVERIFIED: 非公開メソッド名は apidump の Minecraft で確認済み（startAttack()Z / continueAttack(Z)V / startUseItem()V）。実行時に注入できるかは未確認
	@Inject(method = "startAttack()Z", at = @At("HEAD"), cancellable = true)
	private void brawl$noStartAttack(CallbackInfoReturnable<Boolean> cir) {
		if (HudState.blockVanillaInput()) cir.setReturnValue(false);
	}

	@Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
	private void brawl$noContinueAttack(boolean down, CallbackInfo ci) {
		if (HudState.blockVanillaInput()) ci.cancel();
	}

	@Inject(method = "startUseItem()V", at = @At("HEAD"), cancellable = true)
	private void brawl$noUseItem(CallbackInfo ci) {
		if (HudState.blockVanillaInput()) ci.cancel();
	}
}
