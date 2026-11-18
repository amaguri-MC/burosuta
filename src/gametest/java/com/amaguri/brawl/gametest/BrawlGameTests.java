package com.amaguri.brawl.gametest;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.arena.ArenaBlocks;
import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.arena.ArenaMap;
import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;
import com.amaguri.brawl.core.MatchHooks;
import com.amaguri.brawl.mode.Boxes;

/**
 * ブロスタ Mod のサーバー gametest。CONTRACT.md の署名だけを使う（実装の中身は見ない）。
 * 各テストはガラスで囲んだ 16x20 の小さな舞台を、テストごとに別の高さ（FLOOR）に作る。
 * 座標は GameTestHelper の相対座標。実行: ./gradlew runGameTest
 */
public class BrawlGameTests {
	private static final int W = 16;
	private static final int D = 20;
	private static final int H = 6;

	private static final int FLOOR_HIT = 20;
	private static final int FLOOR_AMMO = 40;
	private static final int FLOOR_SUPER = 60;
	private static final int FLOOR_BOX = 80;
	private static final int FLOOR_BUSH = 100;
	private static final int FLOOR_ELIM = 120;
	/** 長い走路（他のテストの舞台と干渉しないよう専用の高さ）。 */
	private static final int FLOOR_RUN = 160;
	private static final int RUN_LEN = 60;

	/** 床は石、周りと天井はガラス、中は空気。立つ高さは floor+1。 */
	private static void buildStage(GameTestHelper h, int floor) {
		BlockPos ca = h.absolutePos(new BlockPos(0, floor, 0));
		BlockPos cb = h.absolutePos(new BlockPos(W - 1, floor, D - 1));
		for (int cx = (ca.getX() >> 4) - 1; cx <= (cb.getX() >> 4) + 1; cx++) {
			for (int cz = (ca.getZ() >> 4) - 1; cz <= (cb.getZ() >> 4) + 1; cz++) {
				h.getLevel().setChunkForced(cx, cz, true);
			}
		}
		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H; y++) {
				for (int z = 0; z < D; z++) {
					boolean edge = x == 0 || x == W - 1 || z == 0 || z == D - 1 || y == H - 1;
					Block b = y == 0 ? Blocks.STONE : edge ? Blocks.GLASS : Blocks.AIR;
					h.setBlock(x, floor + y, z, b);
				}
			}
		}
	}

	/** 舞台にボットを立てて試合参加者にする（AI は止める）。yaw 0 = 南(+z) を向く。 */
	private static BrawlBot bot(GameTestHelper h, int floor, double x, double z, String brawlerId, String name) {
		BrawlBot b = BrawlBot.spawn(h.getLevel(), h.absoluteVec(new Vec3(x, floor + 1, z)), brawlerId, name);
		b.setNoAi(true);
		b.setYRot(0f);
		if (Combat.state(b) == null) {
			Combat.join(b, brawlerId);
		}
		return b;
	}

	private static void moveTo(GameTestHelper h, LivingEntity e, int floor, double x, double z) {
		Vec3 p = h.absoluteVec(new Vec3(x, floor + 1, z));
		e.snapTo(p.x, p.y, p.z, 0f, 0f);
	}

	private static void cleanup(LivingEntity... es) {
		for (LivingEntity e : es) {
			if (Combat.state(e) != null) {
				Combat.leave(e);
			}
			e.discard();
		}
	}

	// ---- 1. ラスティの通常攻撃が正面6ブロックのボットに当たる ----

	@GameTest(maxTicks = 100)
	public void rustyAttackDamagesBotInFront(GameTestHelper h) {
		final int f = FLOOR_HIT;
		buildStage(h, f);
		BrawlBot shooter = bot(h, f, 8.5, 3.5, "rusty", "Shooter");
		BrawlBot target = bot(h, f, 8.5, 9.5, "mortis", "Target");
		final int before = Combat.state(target).hp;
		h.assertTrue(Brawlers.attack(shooter, 0f), "ラスティの通常攻撃が撃てない（弾3発のはず）");
		h.runAfterDelay(40, () -> {
			BrawlState st = Combat.state(target);
			int lost = before - st.hp;
			cleanup(shooter, target);
			h.assertTrue(lost >= 300, "正面6ブロックのボットの hp が 300 以上減るはず: 減少=" + lost);
			h.succeed();
		});
	}

	// ---- 2. 弾3回撃つと4回目は false、リロード後に撃てる ----

	@GameTest(maxTicks = 400)
	public void ammoRunsOutAfterThreeShotsAndReloads(GameTestHelper h) {
		final int f = FLOOR_AMMO;
		buildStage(h, f);
		BrawlBot shooter = bot(h, f, 8.5, 3.5, "rusty", "Ammo");
		BrawlerDef def = Brawlers.get("rusty");
		final int gap = def.attackCooldownTicks() + 1;
		final int reloadTicks = (int) Math.ceil(def.reloadSec() * 20) + 10;
		var st = new Object() {
			int shots;
			long lastShotTick = -1000;
			long emptyAt = -1;
			boolean fourthChecked;
		};
		h.onEachTick(() -> {
			long t = h.getTick();
			if (st.shots < 3) {
				// クールダウンが明けるたびに撃つ。3発成功するまで毎tick試す
				if (t - st.lastShotTick >= gap && Brawlers.attack(shooter, 0f)) {
					st.shots++;
					st.lastShotTick = t;
					if (st.shots == 3) {
						st.emptyAt = t;
					}
				}
				if (t > 200) {
					cleanup(shooter);
					h.fail("3発撃てない: 成功=" + st.shots);
				}
				return;
			}
			// 4発目: クールダウンが明けた直後（弾は 1 未満のはず）
			if (!st.fourthChecked && t - st.emptyAt >= gap) {
				boolean fourth = Brawlers.attack(shooter, 0f);
				float ammo = Combat.state(shooter).ammo;
				if (fourth) {
					cleanup(shooter);
					h.fail("弾が尽きたのに4回目が撃てた: ammo=" + ammo);
				}
				st.fourthChecked = true;
				st.emptyAt = t;
				return;
			}
			// リロード後（reloadSec 経過）に撃てる
			if (st.fourthChecked && t - st.emptyAt >= reloadTicks) {
				boolean again = Brawlers.attack(shooter, 0f);
				float ammo = Combat.state(shooter).ammo;
				cleanup(shooter);
				h.assertTrue(again, "リロード後も撃てない: ammo=" + ammo);
				h.succeed();
			}
		});
	}

	// ---- 3. ゲージ満タンで useSuper → 前方の WALL タイルが消える ----

	@GameTest(maxTicks = 100)
	public void rustySuperBreaksWallTile(GameTestHelper h) {
		final int f = FLOOR_SUPER;
		buildStage(h, f);
		// タイル(2x2)の位置は絶対座標で偶数に揃える
		int x0 = 8;
		int z0 = 8;
		BlockPos abs = h.absolutePos(new BlockPos(x0, f + 1, z0));
		if ((abs.getX() & 1) != 0) {
			x0--;
		}
		if ((abs.getZ() & 1) != 0) {
			z0--;
		}
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) {
				for (int dy = 0; dy < 2; dy++) {
					h.setBlock(x0 + dx, f + 1 + dy, z0 + dz, ArenaBlocks.WALL);
				}
			}
		}
		final BlockPos wallRel = new BlockPos(x0, f + 1, z0);
		h.assertTrue(h.getBlockState(wallRel).is(ArenaBlocks.WALL), "前提: 壁が置かれている");
		BrawlBot shooter = bot(h, f, x0 + 0.5, 3.5, "rusty", "Super");
		Combat.state(shooter).superCharge = 1.0f;
		h.assertTrue(Brawlers.useSuper(shooter, 0f), "ゲージ満タンなのに useSuper が false");
		h.onEachTick(() -> {
			if (!h.getBlockState(wallRel).is(ArenaBlocks.WALL)) {
				cleanup(shooter);
				h.succeed();
			} else if (h.getTick() >= 80) {
				cleanup(shooter);
				h.fail("必殺技のあとも前方の WALL タイルが残っている");
			}
		});
	}

	// ---- 4. 箱を壊す → キューブが出て、近くの参加者が拾う ----

	@GameTest(maxTicks = 300)
	public void brokenBoxDropsCubeAndParticipantPicksItUp(GameTestHelper h) {
		final int f = FLOOR_BOX;
		buildStage(h, f);
		int x0 = 8;
		int z0 = 8;
		BlockPos abs = h.absolutePos(new BlockPos(x0, f + 1, z0));
		if ((abs.getX() & 1) != 0) {
			x0--;
		}
		if ((abs.getZ() & 1) != 0) {
			z0--;
		}
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) {
				for (int dy = 0; dy < 2; dy++) {
					h.setBlock(x0 + dx, f + 1 + dy, z0 + dz, ArenaBlocks.BOX);
				}
			}
		}
		final BlockPos boxRel = new BlockPos(x0, f + 1, z0);
		h.assertTrue(h.getBlockState(boxRel).is(ArenaBlocks.BOX), "前提: 箱が置かれている");
		BrawlBot picker = bot(h, f, 3.5, 4.5, "rusty", "Picker");
		final int baseMax = Brawlers.get("rusty").maxHp();
		var st = new Object() {
			boolean damaged;
			boolean moved;
		};
		h.onEachTick(() -> {
			long t = h.getTick();
			if (!st.damaged) {
				Boxes.damage(h.getLevel(), h.absolutePos(boxRel), 4000, picker);
				st.damaged = true;
				return;
			}
			BrawlState s = Combat.state(picker);
			if (!st.moved) {
				if (h.getBlockState(boxRel).is(ArenaBlocks.BOX)) {
					if (t > 20) {
						cleanup(picker);
						h.fail("箱 HP 4000 のダメージで箱ブロックが消えない");
					}
					return;
				}
				List<ItemEntity> cubes = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(boxRel)).inflate(8),
						e -> e.getItem().is(ArenaBlocks.POWER_CUBE));
				if (cubes.isEmpty() && s != null && s.cubes >= 1) { st.moved = true; return; } // 近くにいて既に拾った
				if (cubes.isEmpty()) {
					if (t > 40) {
						cleanup(picker);
						h.fail("箱が壊れたのに POWER_CUBE が出ない");
					}
					return;
				}
				// 拾う距離（1.2ブロック以内）まで参加者を寄せる
				picker.snapTo(cubes.get(0).position().x, cubes.get(0).position().y, cubes.get(0).position().z, 0f, 0f);
				st.moved = true;
				return;
			}
			if (s.cubes == 1) {
				int max = s.maxHp();
				cleanup(picker);
				h.assertTrue(max == baseMax + Math.round(baseMax * 0.1f), "キューブ1個で最大HPが +10% になるはず: " + max + " (基準 " + baseMax + ")");
				h.succeed();
			} else if (t > 200) {
				cleanup(picker);
				h.fail("近くの参加者がキューブを拾わない: cubes=" + s.cubes);
			}
		});
	}

	// ---- 5. 茂みの中は遠い敵から見えず、近い敵からは見える ----

	@GameTest(maxTicks = 60)
	public void bushHidesFromFarEnemyButNotNear(GameTestHelper h) {
		final int f = FLOOR_BUSH;
		buildStage(h, f);
		for (int dx = 0; dx < 2; dx++) {
			for (int dz = 0; dz < 2; dz++) {
				for (int dy = 0; dy < 2; dy++) {
					h.setBlock(8 + dx, f + 1 + dy, 11 + dz, ArenaBlocks.BUSH);
				}
			}
		}
		BrawlBot hider = bot(h, f, 9.0, 12.0, "mortis", "Hider");
		BrawlBot viewer = bot(h, f, 9.0, 4.0, "rusty", "Viewer");
		boolean inBush = Combat.inBush(hider);
		boolean far = Combat.isHiddenFrom(hider, viewer);
		moveTo(h, viewer, f, 9.0, 9.0);
		boolean near = Combat.isHiddenFrom(hider, viewer);
		cleanup(hider, viewer);
		h.assertTrue(inBush, "前提: 茂みの中にいる");
		h.assertTrue(far, "8ブロック先の敵からは見えない（isHiddenFrom=true）はず");
		h.assertFalse(near, "3ブロックの敵からは見える（isHiddenFrom=false）はず");
		h.succeed();
	}

	// ---- 6. マップ定義 ----

	@GameTest(maxTicks = 20)
	public void mapLayoutHasTenSpawnsAndBoxes(GameTestHelper h) {
		ArenaLayout l = ArenaMap.layout();
		h.assertTrue(l != null, "ArenaMap.layout() が null");
		h.assertTrue(l.spawns().size() == 10, "スタート地点は10個のはず: " + l.spawns().size());
		int boxes = l.boxTiles().size();
		h.assertTrue(boxes >= 14 && boxes <= 18, "箱タイルは 14〜18 個のはず: " + boxes);
		h.succeed();
	}

	// ---- 7. HP が 0 以下で onEliminated がちょうど1回 ----

	@GameTest(maxTicks = 100)
	public void eliminationHookFiresExactlyOnce(GameTestHelper h) {
		final int f = FLOOR_ELIM;
		buildStage(h, f);
		BrawlBot victim = bot(h, f, 8.5, 8.5, "mortis", "Victim");
		var original = MatchHooks.onEliminated;
		int[] count = { 0 };
		LivingEntity[] seen = { null };
		MatchHooks.onEliminated = (v, k) -> {
			count[0]++;
			seen[0] = v;
		};
		try {
			Combat.damage(victim, null, 100000, false);
			Combat.damage(victim, null, 100000, false); // 倒れたあとの追い打ちで二重に呼ばない
		} catch (RuntimeException | Error e) {
			MatchHooks.onEliminated = original;
			throw e;
		}
		h.runAfterDelay(10, () -> {
			int n = count[0];
			LivingEntity who = seen[0];
			MatchHooks.onEliminated = original;
			boolean alive = Combat.state(victim) != null && Combat.state(victim).alive;
			cleanup(victim);
			h.assertTrue(n == 1, "onEliminated はちょうど1回のはず: " + n);
			h.assertTrue(who == victim, "onEliminated に倒れた本人が渡るはず");
			h.assertFalse(alive, "倒れたら alive=false のはず");
			h.succeed();
		});
	}

	// ---- 8. ボットの実測速度が キャラの速さ（タイル/秒 × 2）と ±10% で一致する ----

	@GameTest(maxTicks = 220)
	public void botWalkSpeedMatchesBrawlerSpeed(GameTestHelper h) {
		runSpeedTest(h, FLOOR_RUN, "rusty");
	}

	@GameTest(maxTicks = 220)
	public void botWalkSpeedMatchesFastBrawlerSpeed(GameTestHelper h) {
		runSpeedTest(h, FLOOR_RUN + 20, "mortis");
	}

	private static void runSpeedTest(GameTestHelper h, final int f, final String id) {
		final int w = 7;
		BlockPos ca = h.absolutePos(new BlockPos(0, f, 0));
		BlockPos cb = h.absolutePos(new BlockPos(w - 1, f, RUN_LEN - 1));
		for (int cx = (ca.getX() >> 4) - 1; cx <= (cb.getX() >> 4) + 1; cx++) {
			for (int cz = (ca.getZ() >> 4) - 1; cz <= (cb.getZ() >> 4) + 1; cz++) {
				h.getLevel().setChunkForced(cx, cz, true);
			}
		}
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < H; y++) {
				for (int z = 0; z < RUN_LEN; z++) {
					boolean edge = x == 0 || x == w - 1 || z == 0 || z == RUN_LEN - 1 || y == H - 1;
					h.setBlock(x, f + y, z, y == 0 ? Blocks.STONE : edge ? Blocks.GLASS : Blocks.AIR);
				}
			}
		}
		final double expect = Brawlers.get(id).speedTiles() * 2.0; // ブロック/秒
		// 相棒（召喚物）は敵も主もいなければ AI が歩かないので、走らせる実体にする。主は走路の奥に置く
		BrawlBot owner = bot(h, f, 3.5, RUN_LEN - 3.5, id, "Owner");
		BrawlBot runner = BrawlBot.spawnSummon(h.getLevel(), h.absoluteVec(new Vec3(3.5, f + 1, 3.5)), id, "Runner", owner);
		h.assertTrue(runner != null && Combat.state(runner) != null, "走らせるボットが作れない");
		final Vec3 goal = h.absoluteVec(new Vec3(3.5, f + 1, 33.5)); // 30 ブロック先
		final double[] z = new double[11];
		h.onEachTick(() -> {
			long t = h.getTick();
			if (t == 1) {
				runner.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0);
			}
			if (t >= 1 && (t - 1) % 20 == 0 && (t - 1) / 20 < z.length) {
				int i = (int) ((t - 1) / 20);
				z[i] = runner.getZ();
				System.out.println("[BOTSPEED] t=" + t + " z=" + runner.getZ() + " x=" + runner.getX() + " y=" + runner.getY() + " ground=" + runner.onGround() + " dm=" + runner.getDeltaMovement()
						+ " nav=" + runner.getNavigation().isInProgress() + " below=" + h.getLevel().getBlockState(runner.blockPosition().below()));
			}
			if (t == 1 + 20 * 5) {
				// 2〜5秒目の平均（加速の立ち上がりを除く）
				double bps = (z[5] - z[1]) / 4.0;
				double ratio = bps / expect;
				System.out.println("[BOTSPEED] measured=" + bps + " b/s expect=" + expect + " ratio=" + ratio
						+ " attr=" + runner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
				cleanup(runner, owner);
				h.assertTrue(Math.abs(ratio - 1.0) <= 0.10,
						"ボットの実測速度が キャラの速さと ±10% で一致しない: 実測 " + bps + " b/s / 期待 " + expect + " b/s");
				h.succeed();
			}
		});
	}
}
