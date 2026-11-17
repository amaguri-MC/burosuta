package com.amaguri.brawl.client.model;

import java.util.Map;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * キャラモデル。固定ボーン（head, body, right_arm, left_arm, right_leg, left_leg）を動かす。
 * 動きは形式の座標での「X回転」で書き、ModelPart では符号を逆にして足す（形式で +X = 下端が前へ）。
 */
public final class BrawlerModel extends EntityModel<AvatarRenderState> {
	public final String brawlerId;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public BrawlerModel(ModelPart root, Map<String, ModelPart> parts, String brawlerId) {
		super(root);
		this.brawlerId = brawlerId;
		this.head = parts.get("head");
		this.rightArm = parts.get("right_arm");
		this.leftArm = parts.get("left_arm");
		this.rightLeg = parts.get("right_leg");
		this.leftLeg = parts.get("left_leg");
	}

	private static final float DEG = (float) (Math.PI / 180.0);

	/** 形式の X 回転（ラジアン、+ で下端が前へ）を足す。 */
	private static void swing(ModelPart p, float fmtRad) {
		if (p != null) {
			p.xRot -= fmtRad;
		}
	}

	@Override
	public void setupAnim(AvatarRenderState s) {
		this.resetPose();

		float speed = Math.min(1f, s.walkAnimationSpeed);
		float ph = s.walkAnimationPos * 0.6662f;
		float sw = (float) Math.cos(ph) * 1.2f * speed;
		// 歩き: 脚と腕を逆位相（右脚と右腕、右脚と左脚が逆）
		swing(this.rightLeg, sw);
		swing(this.leftLeg, -sw);
		float armR = -sw;
		float armL = sw;

		// 攻撃: 右腕を前に突き出す（attackTime 0→1 の山形）
		float t = s.attackTime;
		if (t > 0f && t < 1f) {
			float w = (float) Math.sin(Math.PI * t);
			armR = armR * (1f - w) + 1.65f * w;
		}
		swing(this.rightArm, armR);
		swing(this.leftArm, armL);

		// 待機: 小さな上下ゆれ（上方向だけ）と腕のゆらぎ。走っているほど弱い
		float idle = 1f - speed;
		float age = s.ageInTicks;
		this.root.y -= (0.5f + 0.5f * (float) Math.sin(age * 0.12f)) * 0.5f * idle;
		float sway = (float) Math.sin(age * 0.09f) * 0.04f * idle;
		if (this.rightArm != null) {
			this.rightArm.zRot += sway;
		}
		if (this.leftArm != null) {
			this.leftArm.zRot -= sway;
		}

		// 頭: 視線の向き（首の可動域は控えめ）
		if (this.head != null) {
			this.head.yRot += clamp(s.yRot, -35f, 35f) * DEG;
			this.head.xRot += clamp(s.xRot, -25f, 30f) * DEG;
		}
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : Math.min(v, hi);
	}
}
