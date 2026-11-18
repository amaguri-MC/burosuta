package com.amaguri.brawl.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.amaguri.brawl.arena.Arena;
import com.amaguri.brawl.arena.ArenaBlocks;
import com.amaguri.brawl.mode.Boxes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 弾。Combat の毎tick処理から tick() で一括して進める。 */
public final class Projectiles {
	private Projectiles() {}

	/** 当たり判定の刻み幅（ブロック）。 */
	private static final double STEP = 0.25;

	private static final class Active {
		final ServerLevel lvl;
		final Shot shot;
		Vec3 pos;
		double travelled = 0;
		int stepCount = 0;
		final Set<UUID> hit = new HashSet<>();
		boolean done = false;

		Active(ServerLevel lvl, Shot shot) {
			this.lvl = lvl;
			this.shot = shot;
			this.pos = shot.origin();
		}
	}

	private static final List<Active> ACTIVE = new ArrayList<>();
	private static final List<Active> PENDING = new ArrayList<>();

	/** 弾を発射する（次のtickの終わりから進む）。 */
	public static void spawn(ServerLevel lvl, Shot shot) {
		PENDING.add(new Active(lvl, shot));
	}

	static void clear() {
		ACTIVE.clear();
		PENDING.clear();
	}

	static void tick() {
		ACTIVE.addAll(PENDING);
		PENDING.clear();
		for (int i = 0; i < ACTIVE.size(); i++) {
			Active a = ACTIVE.get(i);
			try {
				advance(a);
			} catch (RuntimeException ex) {
				a.done = true; // 1発の失敗で全体を止めない
			}
			// onHit/onEnd 内で spawn された弾を追加（次tickから進む）
		}
		ACTIVE.removeIf(a -> a.done);
		if (!PENDING.isEmpty()) {
			ACTIVE.addAll(PENDING);
			PENDING.clear();
		}
	}

	private static void advance(Active a) {
		Shot s = a.shot;
		if (s.speed() <= 0) {
			finish(a, a.pos, false);
			return;
		}
		double remaining = s.range() - a.travelled;
		double move = Math.min(s.speed(), Math.max(0, remaining));
		int steps = Math.max(1, (int) Math.ceil(move / STEP));
		double stepLen = move / steps;
		for (int i = 0; i < steps && !a.done; i++) {
			Vec3 from = a.pos;
			Vec3 to = from.add(s.dir().scale(stepLen));

			// 1) 参加者との交差（刻みの中で最も近い1人を先に処理）
			List<LivingEntity> hits = new ArrayList<>();
			for (LivingEntity t : Combat.participants(a.lvl)) {
				if (t == s.owner() || Combat.sameSide(s.owner(), t) || a.hit.contains(t.getUUID())) continue;
				AABB box = t.getBoundingBox().inflate(s.radius());
				if (box.contains(from) || box.contains(to) || box.clip(from, to).isPresent()) {
					hits.add(t);
				}
			}
			if (!hits.isEmpty()) {
				hits.sort((x, y) -> Double.compare(x.distanceToSqr(from), y.distanceToSqr(from)));
				for (LivingEntity t : hits) {
					a.hit.add(t.getUUID());
					Vec3 hp = t.position().add(0, t.getBbHeight() * 0.5, 0);
					if (s.onHit() != null) {
						s.onHit().hit(a.lvl, s, t, hp);
					} else {
						Combat.damage(t, s.owner(), s.damage(), s.fromMainAttack());
					}
					if (!s.pierce()) {
						a.pos = to;
						finish(a, hp, false);
						return;
					}
				}
			}

			// 2) ブロック
			BlockPos bp = BlockPos.containing(to.x, to.y, to.z);
			BlockState bs = a.lvl.getBlockState(bp);
			if (Arena.isBulletSolid(bs)) {
				if (bs.getBlock() == ArenaBlocks.BOX) {
					BrawlState os = s.owner() == null ? null : Combat.state(s.owner());
					Boxes.damage(a.lvl, bp, Math.round(s.damage() * (os == null ? 1f : os.damageMul())), s.owner());
					a.pos = to;
					finish(a, to, true);
					return;
				}
				if (s.breaksWalls() && Arena.breakTile(a.lvl, bp)) {
					// 壊して進む
				} else {
					a.pos = from;
					finish(a, to, true);
					return;
				}
			}

			a.pos = to;
			a.travelled += stepLen;
			a.stepCount++;
			if (s.trail() != null && (a.stepCount & 1) == 0) {
				a.lvl.sendParticles(s.trail(), to.x, to.y, to.z, 1, 0, 0, 0, 0);
			}
		}
		if (!a.done && a.travelled >= s.range() - 1e-6) {
			finish(a, a.pos, false);
		}
	}

	private static void finish(Active a, Vec3 pos, boolean hitWall) {
		if (a.done) return;
		a.done = true;
		if (a.shot.onEnd() != null) a.shot.onEnd().end(a.lvl, a.shot, pos, hitWall);
	}
}
