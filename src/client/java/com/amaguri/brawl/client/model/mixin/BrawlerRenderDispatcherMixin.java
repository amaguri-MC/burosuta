package com.amaguri.brawl.client.model.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.server.packs.resources.ResourceManager;

import com.amaguri.brawl.client.model.BrawlerLooks;
import com.amaguri.brawl.client.model.BrawlerModels;
import com.amaguri.brawl.client.model.BrawlerRenderer;
import com.amaguri.brawl.client.model.MobPuppets;

/**
 * priority を小さくして、ボット用(client/bot/mixin/EntityRenderDispatcherMixin)より先に走らせる。
 * 1) 描画器の生成: リロードの最後に、ここで使われるのと同じ Context で BrawlerRenderer を作る。
 * 0) 人形: キャラに対応するバニラのモブがあれば、submit の入口で本体の状態から人形の状態を作って描き、本体の描画は止める。
 * 2) 振り分け: LooksPayload に載った実体（プレイヤーもボットも AvatarRenderState 系）はキャラ描画器へ。
 */
@Mixin(value = EntityRenderDispatcher.class, priority = 500)
public abstract class BrawlerRenderDispatcherMixin {
	private static int brawl$dbg = 0;
	@Shadow @Final private BlockModelResolver blockModelResolver;
	@Shadow @Final private ItemModelResolver itemModelResolver;
	@Shadow @Final private MapRenderer mapRenderer;
	@Shadow @Final private AtlasManager atlasManager;
	@Shadow @Final private Font font;
	@Shadow @Final private Supplier<EntityModelSet> entityModels;
	@Shadow @Final private EquipmentAssetManager equipmentAssets;
	@Shadow @Final private PlayerSkinRenderCache playerSkinRenderCache;

	@Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("TAIL"))
	private void brawl$createBrawlerRenderer(ResourceManager rm, CallbackInfo ci) {
		BrawlerModels.clearCache();
		// UNVERIFIED: Context のコンストラクタ引数の順（dump どおり）
		EntityRendererProvider.Context ctx = new EntityRendererProvider.Context((EntityRenderDispatcher) (Object) this,
				this.blockModelResolver, this.itemModelResolver, this.mapRenderer, rm, this.entityModels.get(),
				this.equipmentAssets, this.atlasManager, this.font, this.playerSkinRenderCache);
		new BrawlerRenderer(ctx); // INSTANCE に入る
	}

	@Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true)
	private void brawl$brawlerRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		// UNVERIFIED: AvatarRenderState.id がプレイヤーでも実体IDになっていること（ボットは BotRenderer が設定）
		if (BrawlerRenderer.INSTANCE != null && state instanceof AvatarRenderState a && BrawlerLooks.modelOf(a.id) != null) {
			cir.setReturnValue(BrawlerRenderer.INSTANCE);
		}
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
			at = @At("HEAD"), cancellable = true)
	private void brawl$puppet(EntityRenderState state, CameraRenderState camera, double dx, double dy, double dz,
			PoseStack poseStack, SubmitNodeCollector collector, CallbackInfo ci) {
		if (!(state instanceof AvatarRenderState a)) {
			return;
		}
		String bid = BrawlerLooks.brawlerOf(a.id);
		if (bid == null || !MobPuppets.has(bid)) {
			return;
		}
		if (a.isSpectator || a.isInvisible) { // 脱落・透明は描かない（今の自作モデルと同じ）
			ci.cancel();
			return;
		}
		EntityRenderDispatcher self = (EntityRenderDispatcher) (Object) this;
		LivingEntityRenderState ps = MobPuppets.build(self, a, bid);
		if ((brawl$dbg++ % 200) == 0) org.slf4j.LoggerFactory.getLogger("brawl-dbg").info("[brawl-dbg] puppet id={} bid={} cls={} d=({},{},{}) light={} scale={} inv={} p=({},{},{})", a.id, bid, a.getClass().getSimpleName(), dx, dy, dz, ps == null ? -1 : ps.lightCoords, ps == null ? -1 : ps.scale, ps == null ? false : ps.isInvisible, a.x, a.y, a.z);
		if (ps == null) {
			return; // 作れなければ今までの描画へ
		}
		self.submit(ps, camera, dx, dy + MobPuppets.hover(bid), dz, poseStack, collector);
		ci.cancel();
	}
}
