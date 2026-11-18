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

			// --- キャラ5人の並び（試合の外。ボットを置いて正面・背面・自分の三人称） ---
			server.runCommand("fill -2 150 -2 18 150 18 minecraft:smooth_stone");
			server.runCommand("tp @a 8 151 1 0 15");
			server.runOnServer(ms -> {
				net.minecraft.server.level.ServerPlayer pl = ms.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel lvl = (net.minecraft.server.level.ServerLevel) pl.level();
				String[] ids = {"rusty", "kaden", "gouen", "mimosa", "takomaru"};
				for (int i = 0; i < ids.length; i++) {
					com.amaguri.brawl.bot.BrawlBot b = com.amaguri.brawl.bot.BrawlBot.spawn(lvl,
							new net.minecraft.world.phys.Vec3(2.5 + i * 3, 151, 7.5), ids[i], ids[i]);
					if (b == null) continue;
					b.setNoAi(true);
					b.setYRot(180f); b.setYHeadRot(180f); b.setYBodyRot(180f);
				}
			});
			context.waitTicks(50);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("lineup_front").disableCounterPrefix());
			server.runCommand("tp @a 8 151 14 180 15");
			context.waitTicks(10);
			context.takeScreenshot(TestScreenshotOptions.of("lineup_back").disableCounterPrefix());
			server.runOnServer(ms -> com.amaguri.brawl.core.Combat.join(ms.getPlayerList().getPlayers().get(0), "kaden"));
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(30);
			context.takeScreenshot(TestScreenshotOptions.of("self_front").disableCounterPrefix());
			context.runOnClient(c -> c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			server.runOnServer(ms -> com.amaguri.brawl.core.Combat.leave(ms.getPlayerList().getPlayers().get(0)));

			server.runCommand("brawl start 9");
			// 3秒カウントダウンが明けてから 3秒後
			context.waitTicks(COUNTDOWN_TICKS + 60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("brawl_hud_3s").disableCounterPrefix());

			// 10秒後
			context.waitTicks(140);
			context.takeScreenshot(TestScreenshotOptions.of("brawl_hud_10s").disableCounterPrefix());
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
							new String[] {"rusty", "kaden", "gouen", "mimosa"}, new int[] {1, 2, 3, 4})));
			context.waitTicks(30);
			context.takeScreenshot(TestScreenshotOptions.of("result_screen").disableCounterPrefix());
			context.runOnClient(c -> c.gui.setScreen(null));
			context.waitTicks(40);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("lobby_after").disableCounterPrefix());
		}
	}
}
