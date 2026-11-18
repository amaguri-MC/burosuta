package com.amaguri.brawl.client.view.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.amaguri.brawl.client.view.TopDown;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;

/** 見下ろし中はカメラの位置・回転を差し替え、detached 扱いにして自分のモデルを描く。 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow private boolean detached;
	@Shadow protected abstract void setRotation(float yRot, float xRot);
	@Shadow protected abstract void setPosition(Vec3 pos);

	// UNVERIFIED: update(DeltaTracker) 末尾で上書きすれば行列/視錐台に反映される想定（setRotation/setPosition が dirty を立てる前提）
	@Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
	private void brawl$topDown(DeltaTracker dt, CallbackInfo ci) {
		Camera self = (Camera) (Object) this;
		float partial = dt.getGameTimeDeltaPartialTick(true);
		TopDown.onCameraUpdate(self, partial, (pos, yaw, pitch) -> {
			this.setRotation(yaw, pitch);
			this.setPosition(pos);
			this.detached = true;
		});
	}
}
