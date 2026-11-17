package com.amaguri.brawl.client.model;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * 試合中のプレイヤーとボットを、キャラのモデルで描く共通の描画器。
 * 描画状態（AvatarRenderState / BotRenderState）は元の描画器が作ったものを受け取るだけ（extract は呼ばれない）。
 * BrawlerRenderDispatcherMixin が getRenderer で振り分ける。
 */
public class BrawlerRenderer extends LivingEntityRenderer<LivingEntity, AvatarRenderState, BrawlerModel> {
	/** BrawlerRenderDispatcherMixin が参照する。 */
	public static BrawlerRenderer INSTANCE;

	public BrawlerRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, emptyModel(), 0.5f);
		INSTANCE = this;
	}

	private static BrawlerModel emptyModel() {
		ModelPart root = LayerDefinition.create(new MeshDefinition(), 64, 64).bakeRoot();
		return new BrawlerModel(root, Map.of(), "");
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return BrawlerModels.textureLocation(this.model.brawlerId);
	}

	@Override
	public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		// 脱落（スペクテイター）・透明は描かない。モデルは submit 時に選ぶ（extract は全員分が先に走るため）
		BrawlerModel m = BrawlerLooks.modelOf(state.id);
		if (m == null || state.isSpectator || state.isInvisible) {
			return;
		}
		this.model = m;
		super.submit(state, poseStack, collector, camera);
	}
}
