package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import com.amaguri.brawl.client.view.TopDown;

/**
 * 見下ろし中は、視錐台による間引き（自分の背中側を描かない仕組み）を止める。
 * カメラを後ろ上空に動かしても、間引きは元の一人称の向きで行われ、手前（背中側）のチャンクや実体が抜けるため。
 * 描画距離の範囲はそのまま（描く量は少し増える）。
 */
@Mixin(Frustum.class)
public abstract class TopDownFrustumMixin {
	@Inject(method = "isVisible(Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
	private void brawl$allVisible(AABB box, CallbackInfoReturnable<Boolean> cir) {
		if (TopDown.active()) cir.setReturnValue(true);
	}

	@Inject(method = "cubeInFrustum(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)I", at = @At("HEAD"), cancellable = true)
	private void brawl$allInside(BoundingBox box, CallbackInfoReturnable<Integer> cir) {
		if (TopDown.active()) cir.setReturnValue(org.joml.FrustumIntersection.INSIDE);
	}

	@Inject(method = "cubeInFrustum(DDDDDD)I", at = @At("HEAD"), cancellable = true)
	private void brawl$allInsideRaw(double x0, double y0, double z0, double x1, double y1, double z1,
			CallbackInfoReturnable<Integer> cir) {
		if (TopDown.active()) cir.setReturnValue(org.joml.FrustumIntersection.INSIDE);
	}

	@Inject(method = "pointInFrustum(DDD)Z", at = @At("HEAD"), cancellable = true)
	private void brawl$pointVisible(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		if (TopDown.active()) cir.setReturnValue(true);
	}
}
