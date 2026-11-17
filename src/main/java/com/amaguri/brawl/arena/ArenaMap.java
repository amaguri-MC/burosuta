package com.amaguri.brawl.arena;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.api.Tiles;

/** W3: data/brawl/maps/showdown.txt を読み、ワールドに建てる。 */
public final class ArenaMap {
	/** 既定の場所（タイル(0,0)の北西下の角。y は床の「上」の高さ＝床ブロックは y-1）。 */
	public static final BlockPos ORIGIN = new BlockPos(10000, 100, 10000);
	/** 壁・茂み・箱の高さ（ブロック）。外壁は BORDER_HEIGHT + バリア。 */
	public static final int HEIGHT = 2;
	static final int BORDER_HEIGHT = 3;
	private static final int BARRIER_TOP = 10;   // 床の上 y+0..y+BARRIER_TOP まで（外壁の上にバリア）
	private static final int CLEAR_TOP = 24;     // 建てる前に空気で消す高さ
	private static final int STONE_DEPTH = 20;   // 床の下を石で埋める深さ
	private static final String RESOURCE = "/data/brawl/maps/showdown.txt";
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	private static char[][] grid; // grid[z][x]
	private static ArenaLayout layout;

	private ArenaMap() {}

	private static synchronized void load() {
		if (grid != null) return;
		List<String> lines = new ArrayList<>();
		try (InputStream in = ArenaMap.class.getResourceAsStream(RESOURCE)) {
			if (in == null) throw new IllegalStateException("map not found: " + RESOURCE);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String l;
			while ((l = br.readLine()) != null) {
				if (!l.isBlank()) lines.add(l.stripTrailing());
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		int d = lines.size();
		int w = lines.isEmpty() ? 0 : lines.get(0).length();
		char[][] g = new char[d][w];
		List<Vec3> spawns = new ArrayList<>();
		List<BlockPos> boxes = new ArrayList<>();
		for (int z = 0; z < d; z++) {
			if (lines.get(z).length() != w) throw new IllegalStateException("map row " + z + " has different width");
			for (int x = 0; x < w; x++) {
				char c = lines.get(z).charAt(x);
				g[z][x] = c;
				int bx = ORIGIN.getX() + (int) Tiles.b(x);
				int bz = ORIGIN.getZ() + (int) Tiles.b(z);
				if (c == 'S') spawns.add(new Vec3(bx + Tiles.BLOCKS_PER_TILE / 2, ORIGIN.getY(), bz + Tiles.BLOCKS_PER_TILE / 2));
				if (c == 'X') boxes.add(new BlockPos(bx, ORIGIN.getY(), bz));
			}
		}
		Vec3 center = new Vec3(ORIGIN.getX() + Tiles.b(w) / 2.0, ORIGIN.getY(), ORIGIN.getZ() + Tiles.b(d) / 2.0);
		layout = new ArenaLayout(ORIGIN, w, d, List.copyOf(spawns), List.copyOf(boxes), center);
		grid = g;
	}

	/** data/brawl/maps/showdown.txt を読む（建てない）。 */
	public static ArenaLayout layout() {
		load();
		return layout;
	}

	/** 建てる（毎試合 作り直し）。範囲をいったん空気で消し、床・石・各ブロックを置く。 */
	public static ArenaLayout build(ServerLevel lvl) {
		load();
		final int ox = ORIGIN.getX(), oy = ORIGIN.getY(), oz = ORIGIN.getZ();
		final int w = layout.widthTiles(), d = layout.depthTiles();
		final int sx = (int) Tiles.b(w), sz = (int) Tiles.b(d); // ブロック数
		final int minY = oy - 1 - STONE_DEPTH;

		// チャンクを読み込み、試合中は保持する（setBlock 前に必要）
		for (int cx = ox >> 4; cx <= (ox + sx - 1) >> 4; cx++) {
			for (int cz = oz >> 4; cz <= (oz + sz - 1) >> 4; cz++) {
				lvl.setChunkForced(cx, cz, true);
				lvl.getChunk(cx, cz);
			}
		}
		// 前の試合の落とし物（キューブなど）を消す
		AABB box = new AABB(ox, minY, oz, ox + sx, oy + CLEAR_TOP, oz + sz);
		for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, box)) ie.discard();

		final BlockState air = Blocks.AIR.defaultBlockState();
		final BlockState stone = Blocks.STONE.defaultBlockState();
		final BlockState barrier = Blocks.BARRIER.defaultBlockState();
		final BlockState floorA = ArenaBlocks.FLOOR_A.defaultBlockState();
		final BlockState floorB = ArenaBlocks.FLOOR_B.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

		// 1) 空気で消す（床より上）
		for (int x = 0; x < sx; x++) {
			for (int z = 0; z < sz; z++) {
				for (int y = oy; y <= oy + CLEAR_TOP; y++) set(lvl, p.set(ox + x, y, oz + z), air);
			}
		}
		// 2) 床の下を石で、床を2色チェックで（タイルごと交互）
		for (int tz = 0; tz < d; tz++) {
			for (int tx = 0; tx < w; tx++) {
				BlockState floor = ((tx + tz) & 1) == 0 ? floorA : floorB;
				for (int dx = 0; dx < 2; dx++) {
					for (int dz = 0; dz < 2; dz++) {
						int x = ox + tx * 2 + dx, z = oz + tz * 2 + dz;
						for (int y = minY; y < oy - 1; y++) set(lvl, p.set(x, y, z), stone);
						set(lvl, p.set(x, oy - 1, z), floor);
					}
				}
			}
		}
		// 3) 壁・茂み・箱・柵・外壁
		for (int tz = 0; tz < d; tz++) {
			for (int tx = 0; tx < w; tx++) {
				char c = grid[tz][tx];
				BlockState st;
				int height;
				switch (c) {
					case '#': st = ArenaBlocks.WALL.defaultBlockState(); height = HEIGHT; break;
					case 'B': st = ArenaBlocks.BUSH.defaultBlockState(); height = HEIGHT; break;
					case 'X': st = ArenaBlocks.BOX.defaultBlockState(); height = HEIGHT; break;
					case 'R': st = ArenaBlocks.ROPE.defaultBlockState(); height = 1; break; // 当たり判定は1.5高さ
					case '=': st = ArenaBlocks.BORDER.defaultBlockState(); height = BORDER_HEIGHT; break;
					default: continue; // '.' 'S'
				}
				for (int dx = 0; dx < 2; dx++) {
					for (int dz = 0; dz < 2; dz++) {
						int x = ox + tx * 2 + dx, z = oz + tz * 2 + dz;
						for (int y = 0; y < height; y++) set(lvl, p.set(x, oy + y, z), st);
						if (c == '=') {
							for (int y = BORDER_HEIGHT; y <= BARRIER_TOP; y++) set(lvl, p.set(x, oy + y, z), barrier);
						}
					}
				}
			}
		}
		return layout;
	}

	private static void set(ServerLevel lvl, BlockPos pos, BlockState st) {
		if (lvl.getBlockState(pos) != st) lvl.setBlock(pos, st, FLAGS);
	}
}
