package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import com.amaguri.brawl.client.view.TopDown;

/** 見下ろし中は一人称の手（持ち物）を描かない。 */
@Mixin(GameRenderer.class)
public abstract class HandHideMixin {
	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void brawl$hideHand(CameraRenderState cam, float partial, org.joml.Matrix4fc proj, CallbackInfo ci) {
		if (TopDown.active()) ci.cancel();
	}
}
