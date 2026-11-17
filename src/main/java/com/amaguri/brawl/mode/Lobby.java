package com.amaguri.brawl.mode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

/** ロビー位置（/brawl setlobby）。config/brawl_lobby.txt に「ディメンション x y z yaw」で保存。無ければワールドスポーン。 */
final class Lobby {
	private Lobby() {}

	private static String dim;
	private static double x, y, z;
	private static float yaw;
	private static boolean set;
	private static boolean loaded;

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("brawl_lobby.txt");
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		try {
			Path f = file();
			if (!Files.exists(f)) return;
			String[] a = Files.readString(f, StandardCharsets.UTF_8).trim().split("\\s+");
			if (a.length < 5) return;
			dim = a[0];
			x = Double.parseDouble(a[1]);
			y = Double.parseDouble(a[2]);
			z = Double.parseDouble(a[3]);
			yaw = Float.parseFloat(a[4]);
			set = true;
		} catch (IOException | RuntimeException e) {
			set = false;
		}
	}

	static void set(ServerPlayer p) {
		load();
		dim = p.level().dimension().identifier().toString();
		x = p.getX();
		y = p.getY();
		z = p.getZ();
		yaw = p.getYRot();
		set = true;
		try {
			Path f = file();
			Files.createDirectories(f.getParent());
			Files.writeString(f, dim + " " + x + " " + y + " " + z + " " + yaw + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			// 保存に失敗しても今回の起動中は有効
		}
	}

	/** プレイヤーをロビー（無ければワールドスポーン）へ送る。 */
	static void send(MinecraftServer server, ServerPlayer p) {
		load();
		ServerLevel lvl = null;
		double tx, ty, tz;
		float tyaw;
		if (set) {
			Identifier id = Identifier.tryParse(dim);
			if (id != null) {
				ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
				lvl = server.getLevel(key);
			}
		}
		if (lvl != null) {
			tx = x;
			ty = y;
			tz = z;
			tyaw = yaw;
		} else {
			ServerLevel ow = server.overworld();
			LevelData.RespawnData rd = ow.getRespawnData();
			lvl = server.getLevel(rd.dimension());
			if (lvl == null) lvl = ow;
			BlockPos sp = rd.pos();
			tx = sp.getX() + 0.5;
			ty = sp.getY();
			tz = sp.getZ() + 0.5;
			tyaw = rd.yaw();
		}
		p.teleportTo(lvl, tx, ty, tz, Set.of(), tyaw, 0f, true);
		p.fallDistance = 0;
	}
}
