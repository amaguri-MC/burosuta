package com.amaguri.brawl.core;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record Shot(LivingEntity owner, Vec3 origin, Vec3 dir /*水平の単位ベクトル*/, double speed /*ブロック/tick*/,
		double range /*ブロック*/, double radius /*当たり判定の太さ ブロック*/, int damage, boolean fromMainAttack,
		boolean breaksWalls, boolean pierce, ParticleOptions trail, OnHit onHit /*null可*/, OnEnd onEnd /*null可*/) {

	/** 指定時は damage を自動では与えない。 */
	public interface OnHit {
		void hit(ServerLevel lvl, Shot s, LivingEntity target, Vec3 pos);
	}

	/** 射程切れ・壁・命中（非貫通）で1回。 */
	public interface OnEnd {
		void end(ServerLevel lvl, Shot s, Vec3 pos, boolean hitWall);
	}
}
