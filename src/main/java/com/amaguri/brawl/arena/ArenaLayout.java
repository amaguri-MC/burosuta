package com.amaguri.brawl.arena;

import net.minecraft.core.BlockPos;

public record ArenaLayout(BlockPos origin /*タイル(0,0)の北西下の角。床の上の高さ*/, int widthTiles, int depthTiles,
    java.util.List<net.minecraft.world.phys.Vec3> spawns /*10個*/, java.util.List<BlockPos> boxTiles /*箱タイルの北西下角*/,
    net.minecraft.world.phys.Vec3 center) {}
