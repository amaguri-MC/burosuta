package com.amaguri.brawl.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** W3: 弾・壁まわりの判定。 */
public final class Arena {
	private Arena() {}

	public static boolean isBulletSolid(BlockState s) {
		if (s.is(ArenaBlocks.WALL) || s.is(ArenaBlocks.BORDER) || s.is(ArenaBlocks.BOX)) return true;
		if (s.is(ArenaBlocks.BUSH) || s.is(ArenaBlocks.ROPE) || s.is(ArenaBlocks.FLOOR_A) || s.is(ArenaBlocks.FLOOR_B)) return false;
		return !s.isAir() && s.isSolidRender(); // バニラの完全ブロック
	}

	public static boolean isBush(BlockState s) {
		return s.is(ArenaBlocks.BUSH);
	}

	/** pos を含む WALL のタイル(2x2x高さ2)を壊す。WALL でなければ（BORDER・BOX 含む）false。 */
	public static boolean breakTile(ServerLevel lvl, BlockPos pos) {
		if (!lvl.getBlockState(pos).is(ArenaBlocks.WALL)) return false;
		BlockPos o = ArenaMap.ORIGIN;
		int bx = o.getX() + Math.floorDiv(pos.getX() - o.getX(), 2) * 2;
		int bz = o.getZ() + Math.floorDiv(pos.getZ() - o.getZ(), 2) * 2;
		boolean any = false;
		for (int dy = -(ArenaMap.HEIGHT - 1); dy < ArenaMap.HEIGHT; dy++) {
			for (int dx = 0; dx < 2; dx++) {
				for (int dz = 0; dz < 2; dz++) {
					BlockPos p = new BlockPos(bx + dx, pos.getY() + dy, bz + dz);
					if (lvl.getBlockState(p).is(ArenaBlocks.WALL)) {
						lvl.destroyBlock(p, false); // 破片パーティクルと音。ドロップなし
						any = true;
					}
				}
			}
		}
		return any;
	}
}
