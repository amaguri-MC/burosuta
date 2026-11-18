package com.amaguri.brawl.client.bot;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.client.model.MobPuppets;

/** ボットを、バニラ既定スキン（Steve/Alex 系を UUID から決める）のプレイヤー姿で描く。名前タグはボット名。 */
public class BotRenderer extends LivingEntityRenderer<BrawlBot, AvatarRenderState, PlayerModel> {
	/** EntityRenderDispatcherMixin が参照する。 */
	public static BotRenderer INSTANCE;
	private final PlayerModel wideModel;
	private final PlayerModel slimModel;

	public BotRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
		this.wideModel = this.model;
		this.slimModel = new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		this.addLayer(new PlayerItemInHandLayer<>(this));
		INSTANCE = this;
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new BotRenderState();
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state.skin.body().texturePath();
	}

	@Override
	public void extractRenderState(BrawlBot entity, AvatarRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);

		// バニラ既定スキンの数種から、実体の UUID で決める（同じボットはいつも同じ見た目）
		PlayerSkin skin = DefaultPlayerSkin.get(entity.getUUID());
		state.skin = skin;
		state.id = entity.getId();
		state.showHat = true;
		state.showJacket = true;
		state.showLeftPants = true;
		state.showRightPants = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
		state.showCape = false;
		state.rightArmPose = armPose(entity.getMainHandItem());
		state.leftArmPose = armPose(entity.getOffhandItem());
	}

	private static HumanoidModel.ArmPose armPose(ItemStack stack) {
		// UNVERIFIED: 持っていれば ITEM(腕を前に出す)、無ければ EMPTY
		return stack.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}

	@Override
	protected void scale(AvatarRenderState state, PoseStack poseStack) {
		poseStack.scale(0.9375f, 0.9375f, 0.9375f); // AvatarRenderer と同じ
	}

	@Override
	protected Component getNameTag(BrawlBot entity) {
		return Component.literal(entity.getBotName());
	}

	@Override
	protected boolean shouldShowName(BrawlBot entity, double distanceSq) {
		return distanceSq < 48.0 * 48.0 && !entity.isInvisible() && !entity.getBotName().isEmpty();
	}

	@Override
	public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		// キャラの人形（バニラのモブ）があればそれを描く。人形が作れない/対応が無いときだけ、下の元のプレイヤー型ボットの見た目
		if (MobPuppets.submitInRenderer(state, poseStack, collector, camera)) {
			return;
		}
		// 太い腕/細い腕のモデルは submit 時に選ぶ（extract は全員分が先に走るため）
		this.model = state.skin.model() == PlayerModelType.SLIM ? this.slimModel : this.wideModel;
		super.submit(state, poseStack, collector, camera);
	}
}
