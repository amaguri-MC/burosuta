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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.server.packs.resources.ResourceManager;

import com.amaguri.brawl.client.model.BrawlerLooks;
import com.amaguri.brawl.client.model.BrawlerModels;
import com.amaguri.brawl.client.model.BrawlerRenderer;

/**
 * priority を小さくして、ボット用(client/bot/mixin/EntityRenderDispatcherMixin)より先に走らせる。
 * 1) 描画器の生成: リロードの最後に、ここで使われるのと同じ Context で BrawlerRenderer を作る。
 * 2) 振り分け: LooksPayload に載った実体（プレイヤーもボットも AvatarRenderState 系）はキャラ描画器へ。
 */
@Mixin(value = EntityRenderDispatcher.class, priority = 500)
public abstract class BrawlerRenderDispatcherMixin {
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
}
