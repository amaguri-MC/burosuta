package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.amaguri.brawl.client.hud.HudState;
import com.amaguri.brawl.client.view.TopDown;

/**
 * 見下ろし中は視錐台の判定が元の一人称の視点で行われ、周りのボット・像が消える。
 * 近く（48ブロック）の生き物は視錐台を無視して描く（茂みに隠れた実体は今までどおり描かない）。
 */
@Mixin(value = EntityRenderDispatcher.class, priority = 1500)
public abstract class TopDownCullMixin {
	@Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z",
			at = @At("HEAD"), cancellable = true)
	private void brawl$topDownCull(Entity entity, Frustum frustum, double x, double y, double z,
			CallbackInfoReturnable<Boolean> cir) {
		if (!TopDown.active() || !(entity instanceof LivingEntity)) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || entity == mc.player || HudState.isHidden(entity)) return;
		if (entity.distanceToSqr(mc.player) < 48 * 48) cir.setReturnValue(true);
	}
}
