package com.amaguri.brawl.client.hud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.amaguri.brawl.client.hud.HudState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

/** 茂みに隠れた実体は描画しない（本体・影・名前タグをまとめて止める）。 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	// UNVERIFIED: shouldRender がレベル描画の実体収集から呼ばれているか（呼ばれていなければ茂みの敵が見える）
	@Inject(method = "shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z",
			at = @At("HEAD"), cancellable = true)
	private void brawl$hideBushed(Entity entity, Frustum frustum, double x, double y, double z,
			CallbackInfoReturnable<Boolean> cir) {
		if (entity != Minecraft.getInstance().player && HudState.isHidden(entity)) cir.setReturnValue(false);
	}
}
