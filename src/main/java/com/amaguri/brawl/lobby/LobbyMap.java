package com.amaguri.brawl.lobby;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * ロビー広場（44×44）を data/brawl/lobby/plaza.txt から建てる。文字の意味は agent/preview/L/gen_plaza.py の凡例と同じ。
 * 床の上の高さ y=100（床ブロックは y=99）。北西の角が (9978, 100, 9818)、中心が x=10000, z=9840。
 */
public final class LobbyMap {
	private LobbyMap() {}

	public static final BlockPos ORIGIN = new BlockPos(9978, 100, 9818);
	private static final String RESOURCE = "/data/brawl/lobby/plaza.txt";
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
	private static final int STONE_DEPTH = 8;    // 床の下を石で埋める深さ
	private static final int CLEAR_TOP = 26;     // 建てる前に空気で消す高さ
	private static final int BARRIER_TOP = 7;    // 外壁(2高)の上に見えない壁を y+2..y+BARRIER_TOP まで

	private static char[][] grid;               // grid[z][x]
	private static LobbyLayout layout;

	private static synchronized void load() {
		if (grid != null) return;
		List<String> lines = new ArrayList<>();
		try (InputStream in = LobbyMap.class.getResourceAsStream(RESOURCE)) {
			if (in == null) throw new IllegalStateException("lobby map not found: " + RESOURCE);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String l;
			while ((l = br.readLine()) != null) {
				if (!l.isBlank()) lines.add(l.stripTrailing());
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		int n = lines.size();
		char[][] g = new char[n][];
		for (int z = 0; z < n; z++) {
			if (lines.get(z).length() != n) throw new IllegalStateException("lobby map must be square: row " + z);
			g[z] = lines.get(z).toCharArray();
		}
		BlockPos o = ORIGIN;
		Vec3 spawn = new Vec3(o.getX() + n / 2 + 0.5, o.getY(), o.getZ() + n - 6 + 0.5);
		List<Vec3> statues = new ArrayList<>();
		List<Vec3> dummies = new ArrayList<>();
		int padMinI = Integer.MAX_VALUE, padMaxI = -1, padMinJ = Integer.MAX_VALUE, padMaxJ = -1;
		int kMinI = Integer.MAX_VALUE, kMinJ = Integer.MAX_VALUE, kMaxJ = -1;
		int mMinI = Integer.MAX_VALUE, mMaxI = -1, mMinJ = Integer.MAX_VALUE;
		for (int i = 0; i < n; i++) {       // 西→東
			for (int j = 0; j < n; j++) {   // 北→南
				char c = g[j][i];
				double x = o.getX() + i + 0.5, z = o.getZ() + j + 0.5;
				switch (c) {
					case 'S' -> spawn = new Vec3(x, o.getY(), z);
					case 'N' -> statues.add(new Vec3(x, o.getY() + 2, z));
					case 'P' -> {
						padMinI = Math.min(padMinI, i); padMaxI = Math.max(padMaxI, i);
						padMinJ = Math.min(padMinJ, j); padMaxJ = Math.max(padMaxJ, j);
					}
					case 'K' -> { kMinI = Math.min(kMinI, i); kMinJ = Math.min(kMinJ, j); kMaxJ = Math.max(kMaxJ, j); }
					case 'g' -> { mMinI = Math.min(mMinI, i); mMaxI = Math.max(mMaxI, i); mMinJ = Math.min(mMinJ, j); }
					default -> { }
				}
			}
		}
		for (int j = 0; j < n; j++) {       // カカシは北→南
			for (int i = 0; i < n; i++) {
				if (g[j][i] == 'D') dummies.add(new Vec3(o.getX() + i + 0.5, o.getY(), o.getZ() + j + 0.5));
			}
		}
		if (padMaxI < 0) throw new IllegalStateException("lobby map has no pad (P)");
		double fy = o.getY();
		AABB pad = new AABB(o.getX() + padMinI, fy - 0.2, o.getZ() + padMinJ,
				o.getX() + padMaxI + 1, fy + 1.6, o.getZ() + padMaxJ + 1);
		Vec3 padText = new Vec3((pad.minX + pad.maxX) / 2, fy + 2.0, (pad.minZ + pad.maxZ) / 2);
		Vec3 boardText = kMaxJ < 0 ? spawn.add(0, 3, 0)
				: new Vec3(o.getX() + kMinI - 0.7, fy + 2.4, o.getZ() + (kMinJ + kMaxJ + 1) / 2.0);
		Vec3 practiceText = mMaxI < 0 ? spawn.add(0, 3, 0)
				: new Vec3(o.getX() + (mMinI + mMaxI + 1) / 2.0, fy + 1.6, o.getZ() + mMinJ - 1.0);
		AABB bounds = new AABB(o.getX() - 1, fy - STONE_DEPTH - 2, o.getZ() - 1, o.getX() + n + 1, fy + 40, o.getZ() + n + 1);
		layout = new LobbyLayout(o, n, spawn, 180f, List.copyOf(statues), List.copyOf(dummies), pad, padText,
				boardText, practiceText, bounds);
		grid = g;
	}

	/** 配置を返す（建てない）。 */
	public static LobbyLayout layout() {
		load();
		return layout;
	}

	/** 広場を建てる（何度呼んでも同じ結果。毎回作り直し）。実体（像・カカシ・文字）は {@link LobbyEntities} が置く。 */
	public static LobbyLayout build(ServerLevel lvl) {
		load();
		int n = layout.size();
		BlockPos o = layout.origin();
		int fy = o.getY();
		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState stone = Blocks.STONE.defaultBlockState();

		// 広場の4×4チャンクぶんを常に読み込む（像やカカシが消えないように）
		for (int cx = (o.getX() - 1) >> 4; cx <= (o.getX() + n) >> 4; cx++) {
			for (int cz = (o.getZ() - 1) >> 4; cz <= (o.getZ() + n) >> 4; cz++) {
				lvl.setChunkForced(cx, cz, true);
			}
		}

		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				int x = o.getX() + i, z = o.getZ() + j;
				for (int y = fy - STONE_DEPTH; y < fy - 1; y++) set(lvl, x, y, z, stone);
				for (int y = fy; y <= fy + CLEAR_TOP; y++) set(lvl, x, y, z, air);
			}
		}
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				cell(lvl, i, j, grid[j][i]);
			}
		}
		return layout;
	}

	private static void set(ServerLevel lvl, int x, int y, int z, BlockState s) {
		lvl.setBlock(new BlockPos(x, y, z), s, FLAGS);
	}

	private static void cell(ServerLevel lvl, int i, int j, char c) {
		BlockPos o = layout.origin();
		int x = o.getX() + i, z = o.getZ() + j, fy = o.getY();
		BlockState tileA = LobbyBlocks.TILE_A.defaultBlockState();
		BlockState tileB = LobbyBlocks.TILE_B.defaultBlockState();
		BlockState checker = ((i / 2 + j / 2) % 2 == 0) ? tileA : tileB;
		BlockState floor = checker;                  // y = fy-1 に置く床
		switch (c) {
			case 'W' -> {
				floor = LobbyBlocks.EDGE.defaultBlockState();
				set(lvl, x, fy, z, floor);
				set(lvl, x, fy + 1, z, floor);
				for (int y = fy + 2; y <= fy + BARRIER_TOP; y++) set(lvl, x, y, z, Blocks.BARRIER.defaultBlockState());
			}
			case 'e' -> floor = LobbyBlocks.EDGE.defaultBlockState();
			case 'c' -> floor = Blocks.CONCRETE.lightBlue().defaultBlockState();
			case 'y' -> floor = Blocks.CONCRETE.yellow().defaultBlockState();
			case 'g' -> floor = Blocks.CONCRETE.lime().defaultBlockState();
			case 'p' -> floor = Blocks.CONCRETE.white().defaultBlockState();
			case 'P' -> floor = LobbyBlocks.PAD.defaultBlockState();
			case 'D' -> floor = Blocks.CONCRETE.red().defaultBlockState();
			case 'd' -> floor = Blocks.CONCRETE.white().defaultBlockState();
			case 'a', 'S' -> floor = tileA;
			case 'b' -> floor = tileB;
			case 'T', 'N' -> {
				floor = LobbyBlocks.PEDESTAL.defaultBlockState();
				set(lvl, x, fy, z, floor);
				set(lvl, x, fy + 1, z, floor);
			}
			case 'K' -> {
				floor = LobbyBlocks.PEDESTAL.defaultBlockState();
				for (int y = fy; y <= fy + 4; y++) set(lvl, x, y, z, floor);
			}
			case 'q' -> {
				floor = Blocks.CONCRETE.orange().defaultBlockState();
				set(lvl, x, fy, z, floor);
			}
			case 'm' -> {
				floor = Blocks.MOSS_BLOCK.defaultBlockState();
				Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.CORNFLOWER, Blocks.BLUE_ORCHID};
				set(lvl, x, fy, z, flowers[(i * 3 + j * 5) % flowers.length].defaultBlockState());
			}
			case 'V' -> {
				floor = Blocks.MOSS_BLOCK.defaultBlockState();
				BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
				set(lvl, x, fy, z, leaves);
				set(lvl, x, fy + 1, z, leaves);
			}
			case 'B', 'C' -> {
				floor = tileA;
				Direction face = c == 'B' ? Direction.NORTH : Direction.SOUTH;
				set(lvl, x, fy, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, face));
			}
			case 'M' -> {
				floor = checker;
				for (int y = fy; y <= fy + 3; y++) set(lvl, x, y, z, Blocks.BIRCH_FENCE.defaultBlockState());
				set(lvl, x, fy + 4, z, Blocks.SEA_LANTERN.defaultBlockState());
			}
			case 'R', 'Y', 'G', 'U' -> {
				floor = checker;
				for (int y = fy; y <= fy + 6; y++) set(lvl, x, y, z, Blocks.BIRCH_FENCE.defaultBlockState());
				BlockState wool = switch (c) {
					case 'R' -> Blocks.WOOL.red().defaultBlockState();
					case 'Y' -> Blocks.WOOL.yellow().defaultBlockState();
					case 'G' -> Blocks.WOOL.lime().defaultBlockState();
					default -> Blocks.WOOL.blue().defaultBlockState();
				};
				for (int k = 1; k <= 2; k++) {              // 旗は東へ2マス、2段
					set(lvl, x + k, fy + 4, z, wool);
					set(lvl, x + k, fy + 5, z, wool);
				}
			}
			default -> { }
		}
		set(lvl, x, fy - 1, z, floor);
	}
}
