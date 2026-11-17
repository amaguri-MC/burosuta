package com.amaguri.brawl.client.bot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.amaguri.brawl.client.bot.BotRenderState;
import com.amaguri.brawl.client.bot.BotRenderer;

/**
 * 26.2 の EntityRenderDispatcher は「描画状態」から描画器を選び、AvatarRenderState 系はプレイヤー用の AvatarRenderer に回す。
 * ボットの状態(BotRenderState)は自分の描画器に回し直す。
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true)
	private void brawl$botRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (state instanceof BotRenderState && BotRenderer.INSTANCE != null) {
			cir.setReturnValue(BotRenderer.INSTANCE);
		}
	}
}
