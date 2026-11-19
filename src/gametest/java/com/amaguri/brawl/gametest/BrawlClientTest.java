package com.amaguri.brawl.gametest;

import java.util.Locale;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

import com.amaguri.brawl.arena.ArenaLayout;
import com.amaguri.brawl.arena.ArenaMap;

/**
 * 実機クライアントで /brawl start（ボット9）→ カウントダウン後 3秒・10秒の HUD 入りスクショ、
 * マップ全体を真上からのスペクテイター視点で1枚（run/screenshots/*.png）。撮影後 /brawl stop。
 * 実行: xvfb-run ./gradlew runClientGameTest
 */
public class BrawlClientTest implements FabricClientGameTest {
	private static final int COUNTDOWN_TICKS = 60; // CONTRACT: 3秒カウントダウン

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(c -> {
			c.options.languageCode = "ja_jp";
			c.options.renderDistance().set(6);
			// UNVERIFIED: 26.2 の Options に hideGui フィールドが apidump に無い。既定で HUD は出る（隠さない）
		});
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksDownload();
			TestServerContext server = sp.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");

			// --- ロビー（入った直後はロビー。見下ろし視点）・キャラ選択画面 ---
			context.waitTicks(60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("lobby_topdown").disableCounterPrefix());
			server.runOnServer(ms -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
					ms.getPlayerList().getPlayers().get(0), new com.amaguri.brawl.api.Net.OpenSelectPayload("mimosa")));
			context.waitTicks(40);
			context.takeScreenshot(TestScreenshotOptions.of("select_screen").disableCounterPrefix());
			context.runOnClient(c -> c.gui.setScreen(null));
			context.waitTicks(5);

			server.runCommand("difficulty peaceful"); // ユーザー環境（ピースフル）で敵モブの見た目が出ないことがあった
			// --- キャラ7人の並び（ロビーの中、プレイヤーの北側に一列。見下ろし視点で撮る） ---
			server.runOnServer(ms -> {
				net.minecraft.server.level.ServerPlayer pl = ms.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel lvl = (net.minecraft.server.level.ServerLevel) pl.level();
				String[] ids = {"rusty", "nita", "gouen", "frank", "darryl", "mimosa", "barley", "poco", "mortis", "edgar", "lou"};
				for (int i = 0; i < ids.length; i++) {
					com.amaguri.brawl.bot.BrawlBot b = com.amaguri.brawl.bot.BrawlBot.spawn(lvl,
							pl.position().add(-10 + i * 2, 0, -4), ids[i], ids[i]);
					if (b == null) continue;
					b.setNoAi(true);
					b.setYRot(0f); b.setYHeadRot(0f); b.setYBodyRot(0f);
				}
			});
			context.waitTicks(50);
			context.takeScreenshot(TestScreenshotOptions.of("lineup_lobby").disableCounterPrefix());
			context.runOnClient(c -> com.amaguri.brawl.client.view.TopDown.enabled = false);
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~4 180 15");
			context.waitTicks(20);
			context.takeScreenshot(TestScreenshotOptions.of("lineup_fp").disableCounterPrefix());
			// --- プレイヤー自身がその3体を使ったときの姿（三人称の正面） ---
			server.runCommand("kill @e[type=brawl:bot,tag=!brawl_lobby]");
			for (String sid : new String[] {"edgar", "barley", "mimosa"}) {
				server.runCommand("execute as @p run brawl pick " + sid);
				context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
				context.waitTicks(30);
				context.takeScreenshot(TestScreenshotOptions.of("self_" + sid).disableCounterPrefix());
			}
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			server.runCommand("execute as @p run brawl pick rusty");
			for (String sid : new String[] {"edgar", "barley", "mimosa"}) {
				server.runOnServer(ms -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
						ms.getPlayerList().getPlayers().get(0), new com.amaguri.brawl.api.Net.OpenSelectPayload(sid)));
				context.waitTicks(30);
				context.takeScreenshot(TestScreenshotOptions.of("select_" + sid).disableCounterPrefix());
				context.runOnClient(c -> c.gui.setScreen(null));
				context.waitTicks(5);
			}
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~-4");
			context.runOnClient(c -> com.amaguri.brawl.client.view.TopDown.enabled = true);
			server.runCommand("kill @e[type=brawl:bot,tag=!brawl_lobby]");
			context.waitTicks(5);
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~-16");
			context.waitTicks(40);
			context.takeScreenshot(TestScreenshotOptions.of("statues").disableCounterPrefix());
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~16");
			context.waitTicks(10);

			// --- FV: 3体（ウィザースケルトン/ウィッチ/ブレイズ）を、バニラの同じモブの隣に置いて近くで撮る（正面・横）。他のモブが写らないよう上空の台に置く ---
			context.runOnClient(c -> com.amaguri.brawl.client.view.TopDown.enabled = false);
			server.runCommand("execute as @a at @s run fill ~-8 ~29 ~-12 ~8 ~29 ~3 minecraft:white_concrete");
			server.runCommand("execute as @a at @s run tp @s ~ ~30 ~ 180 8");
			context.waitTicks(10);
			server.runOnServer(ms -> {
				net.minecraft.server.level.ServerPlayer pl = ms.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel lvl = (net.minecraft.server.level.ServerLevel) pl.level();
				String[] ids = {"edgar", "barley", "mimosa"};
				for (int i = 0; i < ids.length; i++) {
					com.amaguri.brawl.bot.BrawlBot b = com.amaguri.brawl.bot.BrawlBot.spawn(lvl,
							pl.position().add(-4.5 + i * 3.2, 0, -5), ids[i], ids[i]);
					if (b == null) continue;
					b.setNoAi(true);
					b.addTag("fvt");
					b.setYRot(0f); b.setYHeadRot(0f); b.setYBodyRot(0f);
				}
			});
			server.runCommand("execute as @a at @s run summon minecraft:wither_skeleton ~-3 ~ ~-5 {NoAI:1b,Rotation:[0f,0f],Tags:[\"fvt\"],PersistenceRequired:1b}");
			server.runCommand("execute as @a at @s run summon minecraft:witch ~0.2 ~ ~-5 {NoAI:1b,Rotation:[0f,0f],Tags:[\"fvt\"],PersistenceRequired:1b}");
			server.runCommand("execute as @a at @s run summon minecraft:blaze ~3.4 ~ ~-5 {NoAI:1b,Rotation:[0f,0f],Tags:[\"fvt\"],PersistenceRequired:1b}");
			context.waitTicks(40);
			context.takeScreenshot(TestScreenshotOptions.of("fv_front").disableCounterPrefix());
			server.runCommand("execute as @e[tag=fvt] run rotate @s 90 0");
			context.waitTicks(20);
			context.takeScreenshot(TestScreenshotOptions.of("fv_side").disableCounterPrefix());
			server.runCommand("kill @e[tag=fvt]");
			server.runCommand("execute as @a at @s run tp @s ~ ~-30 ~");
			context.runOnClient(c -> com.amaguri.brawl.client.view.TopDown.enabled = true);
			context.waitTicks(5);

			// --- プレイヤーの実際の歩く速さを測る（平らな台で W を2秒） ---
			server.runCommand("fill -40 150 -40 40 150 40 minecraft:smooth_stone");
			server.runCommand("tp @a 0 151 30 180 0");
			context.waitTicks(10);
			server.runOnServer(ms -> com.amaguri.brawl.core.Combat.join(ms.getPlayerList().getPlayers().get(0), "rusty"));
			context.waitTicks(10);
			double[] p0 = new double[3];
			context.runOnClient(c -> { p0[0] = c.player.getX(); p0[2] = c.player.getZ(); });
			context.getInput().holdKey(o -> o.keyUp);
			for (int k = 0; k < 12; k++) {
				context.waitTicks(5);
				context.runOnClient(c -> org.slf4j.LoggerFactory.getLogger("brawl-speed").info(
						"[brawl-speed] t z={} x={} y={} onGround={} dm={}", c.player.getZ(), c.player.getX(), c.player.getY(),
						c.player.onGround(), c.player.getDeltaMovement()));
			}
			context.getInput().releaseKey(o -> o.keyUp);
			context.runOnClient(c -> org.slf4j.LoggerFactory.getLogger("brawl-speed").info(
					"[brawl-speed] player walked {} blocks in 2s => {} b/s (state={}) clientAttr={}",
					Math.hypot(c.player.getX() - p0[0], c.player.getZ() - p0[2]),
					Math.hypot(c.player.getX() - p0[0], c.player.getZ() - p0[2]) / 2.0,
					com.amaguri.brawl.client.hud.HudState.hud == null ? "null" : com.amaguri.brawl.client.hud.HudState.hud.brawler(),
					c.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)));
			server.runOnServer(ms -> com.amaguri.brawl.core.Combat.leave(ms.getPlayerList().getPlayers().get(0)));
			server.runCommand("difficulty normal");
			server.runCommand("brawl start 9");
			// 3秒カウントダウンが明けてから 3秒後
			context.waitTicks(COUNTDOWN_TICKS + 60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("brawl_hud_3s").disableCounterPrefix());

			// 10秒後
			context.waitTicks(140);
			context.takeScreenshot(TestScreenshotOptions.of("brawl_hud_10s").disableCounterPrefix());
			// --- 箱のHP表示（一番目の箱の南4ブロックへ移動し、少し削る） ---
			server.runOnServer(ms -> {
				net.minecraft.server.level.ServerPlayer pl = ms.getPlayerList().getPlayers().get(0);
				net.minecraft.core.BlockPos bt = com.amaguri.brawl.arena.ArenaMap.layout().boxTiles().get(0);
				pl.teleportTo(bt.getX() + 1.0, bt.getY(), bt.getZ() + 6.0);
				com.amaguri.brawl.mode.Boxes.damage((net.minecraft.server.level.ServerLevel) pl.level(), bt, 2500, null);
			});
			context.waitTicks(25);
			context.takeScreenshot(TestScreenshotOptions.of("box_hp").disableCounterPrefix());
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
			context.waitTicks(5);
			context.takeScreenshot(TestScreenshotOptions.of("brawl_third_person").disableCounterPrefix());
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

			// マップ全体を真上から（スペクテイター）。100x100 ブロックが収まる高さ
			ArenaLayout layout = ArenaMap.layout();
			double cx = layout.center().x;
			double cy = layout.center().y + 80;
			double cz = layout.center().z;
			server.runCommand("execute as @a run gamemode spectator");
			server.runCommand(String.format(Locale.ROOT, "execute as @a run tp @s %.1f %.1f %.1f 0 90", cx, cy, cz));
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("brawl_map_top").disableCounterPrefix());

			server.runCommand("brawl stop");
			context.waitTicks(20);
			// --- 結果画面（試合終了時に送られるものを模擬） ---
			server.runOnServer(ms -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
					ms.getPlayerList().getPlayers().get(0), new com.amaguri.brawl.api.Net.ResultPayload(1, 10, 10, 130,
							new String[] {"Player", "ボット1", "ボット2", "ボット3"},
							new String[] {"rusty", "frank", "gouen", "mimosa"}, new int[] {1, 2, 3, 4})));
			context.waitTicks(30);
			context.takeScreenshot(TestScreenshotOptions.of("result_screen").disableCounterPrefix());
			context.runOnClient(c -> c.gui.setScreen(null));
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("lobby_after").disableCounterPrefix());
		}
	}
}
