package com.amaguri.brawl.mode;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.amaguri.brawl.arena.ArenaBlocks;
import com.amaguri.brawl.core.Combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** パワーキューブ（ArenaBlocks.POWER_CUBE の ItemEntity）の生成と、近づいた参加者への付与。 */
final class Cubes {
	private Cubes() {}

	static final double PICKUP_RANGE = 1.2;

	static void drop(ServerLevel lvl, Vec3 pos, int count) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			ItemEntity ie = new ItemEntity(lvl, pos.x, pos.y, pos.z, new ItemStack(ArenaBlocks.POWER_CUBE));
			ie.setNeverPickUp();           // 拾えない（バニラの拾得は無効。近づいたら下の pickup が処理）
			ie.setUnlimitedLifetime();     // 消えない
			double a = r.nextDouble() * Math.PI * 2;
			double s = count == 1 ? 0.0 : 0.12 + r.nextDouble() * 0.12;
			ie.setDeltaMovement(Math.cos(a) * s, 0.25 + r.nextDouble() * 0.1, Math.sin(a) * s);
			lvl.addFreshEntity(ie);
		}
	}

	/** 試合外用: 参加者の近くのキューブだけ拾わせる（他のアイテムは触らない）。 */
	static void pickupNear(ServerLevel lvl, List<LivingEntity> ps) {
		for (LivingEntity e : ps) {
			AABB box = e.getBoundingBox().inflate(PICKUP_RANGE, 1.0, PICKUP_RANGE);
			tick(lvl, box, List.of(e), true);
		}
	}

	static int tick(ServerLevel lvl, AABB arena, List<LivingEntity> candidates) {
		return tick(lvl, arena, candidates, false);
	}

	/** 毎tick: アリーナ内のアイテムを調べる。キューブは近くの参加者へ、それ以外（投げ捨てた物）は消す。戻り値=消した非キューブの数。 */
	private static int tick(ServerLevel lvl, AABB arena, List<LivingEntity> candidates, boolean cubesOnly) {
		int removedOther = 0;
		for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, arena)) {
			if (ie.isRemoved()) continue;
			ItemStack st = ie.getItem();
			if (st.getItem() != ArenaBlocks.POWER_CUBE) {
				if (cubesOnly) continue;
				ie.discard();
				removedOther++;
				continue;
			}
			LivingEntity taker = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity e : candidates) {
				double dx = e.getX() - ie.getX();
				double dz = e.getZ() - ie.getZ();
				double dy = e.getY() + e.getBbHeight() * 0.5 - ie.getY();
				double h2 = dx * dx + dz * dz;
				if (h2 <= PICKUP_RANGE * PICKUP_RANGE && Math.abs(dy) <= 2.0 && h2 < best) {
					best = h2;
					taker = e;
				}
			}
			if (taker == null) continue;
			int n = Math.max(1, st.getCount());
			ie.discard();
			for (int i = 0; i < n; i++) Combat.addCube(taker);
			lvl.playSound(null, taker.getX(), taker.getY(), taker.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
					SoundSource.PLAYERS, 0.8f, 1.2f);
			lvl.sendParticles(ParticleTypes.HAPPY_VILLAGER, taker.getX(), taker.getY() + 1.0, taker.getZ(), 6, 0.3, 0.4, 0.3, 0.0);
		}
		return removedOther;
	}
}
