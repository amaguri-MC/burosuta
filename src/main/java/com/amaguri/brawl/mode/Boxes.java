package com.amaguri.brawl.mode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amaguri.brawl.arena.ArenaBlocks;
import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.arena.ArenaMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** パワーキューブ箱（2x2x2 の BOX ブロック）。HP 4000。 */
public final class Boxes {
	private Boxes() {}

	static final int BOX_HP = 6000;

	/** 箱の北西下角（boxTiles の要素）→ 残りHP。まだ殴られていない箱は載らない（=満タン）。 */
	private static final Map<Long, Integer> HP = new HashMap<>();
	private static ArenaLayout fallback; // 試合外で呼ばれたときの読み込み結果

	/** 試合の開始・終了で呼ぶ。 */
	static void reset() {
		HP.clear();
		fallback = null;
	}

	private static ArenaLayout layout() {
		ArenaLayout l = Showdown.currentLayout();
		if (l != null) return l;
		if (fallback == null) fallback = ArenaMap.layout();
		return fallback;
	}

	/** pos を含む箱の北西下角。箱でなければ null。 */
	private static BlockPos find(BlockPos pos) {
		ArenaLayout l = layout();
		if (l == null) return null;
		List<BlockPos> tiles = l.boxTiles();
		for (BlockPos b : tiles) {
			if (pos.getX() >= b.getX() && pos.getX() <= b.getX() + 1
					&& pos.getZ() >= b.getZ() && pos.getZ() <= b.getZ() + 1
					&& pos.getY() >= b.getY() && pos.getY() <= b.getY() + 1) {
				return b;
			}
		}
		return null;
	}

	/** マップ外（テストなど）の箱: 隣の BOX から 2x2x2 の北西下角を求める。 */
	private static BlockPos findLoose(ServerLevel lvl, BlockPos pos) {
		if (lvl.getBlockState(pos).getBlock() != ArenaBlocks.BOX) return null;
		int x = lvl.getBlockState(pos.west()).getBlock() == ArenaBlocks.BOX ? pos.getX() - 1 : pos.getX();
		int y = lvl.getBlockState(pos.below()).getBlock() == ArenaBlocks.BOX ? pos.getY() - 1 : pos.getY();
		int z = lvl.getBlockState(pos.north()).getBlock() == ArenaBlocks.BOX ? pos.getZ() - 1 : pos.getZ();
		return new BlockPos(x, y, z);
	}

	public static void damage(ServerLevel lvl, BlockPos pos, int dmg, LivingEntity by) {
		BlockPos b = find(pos);
		if (b == null) b = findLoose(lvl, pos);
		if (b == null) return;
		if (lvl.getBlockState(b).getBlock() != ArenaBlocks.BOX && lvl.getBlockState(pos).getBlock() != ArenaBlocks.BOX) {
			return; // すでに壊れている
		}
		long key = b.asLong();
		int hp = HP.getOrDefault(key, BOX_HP) - Math.max(0, dmg);
		Vec3 c = new Vec3(b.getX() + 1.0, b.getY() + 1.0, b.getZ() + 1.0);
		BlockParticleOption wood = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState());
		if (hp > 0) {
			HP.put(key, hp);
			lvl.playSound(null, c.x, c.y, c.z, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
			lvl.sendParticles(wood, c.x, c.y, c.z, 12, 0.7, 0.7, 0.7, 0.05);
			return;
		}
		HP.remove(key);
		org.slf4j.LoggerFactory.getLogger("brawl").info("[box] broken at {}", b);
		for (int dx = 0; dx < 2; dx++) {
			for (int dy = 0; dy < 2; dy++) {
				for (int dz = 0; dz < 2; dz++) {
					lvl.setBlock(b.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		lvl.playSound(null, c.x, c.y, c.z, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.2f, 0.9f);
		lvl.sendParticles(wood, c.x, c.y, c.z, 40, 0.9, 0.9, 0.9, 0.1);
		Cubes.drop(lvl, c, 1);
	}
}
