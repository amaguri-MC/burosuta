package com.amaguri.brawl.client.hud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.amaguri.brawl.client.hud.HudState;

/** 試合中はバニラの名前タグを消す（自前の頭上表示に置き換え）。 */
@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public abstract class LivingEntityRendererNameMixin {
	// UNVERIFIED: 描画器ごとに shouldShowName をオーバーライドしているため、継承元ごとに注入（require=0 で無ければ無視）
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void brawl$noNameTag(net.minecraft.world.entity.LivingEntity entity, double dist, CallbackInfoReturnable<Boolean> cir) {
		if (HudState.replacesNameTag(entity)) cir.setReturnValue(false);
	}
}
