package com.amaguri.brawl.lobby;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * ロビー広場の配置（data/brawl/lobby/plaza.txt から読む。建てない）。
 * origin=北西の角（床の「上」の高さ）。statues は西→東の順、dummies は北→南の順。
 */
public record LobbyLayout(BlockPos origin, int size, Vec3 spawn, float spawnYaw, List<Vec3> statues,
		List<Vec3> dummies, AABB pad, Vec3 padText, Vec3 boardText, Vec3 practiceText, AABB bounds) {}
