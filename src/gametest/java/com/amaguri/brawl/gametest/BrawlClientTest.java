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

			// --- キャラ7人の並び（ロビーの中、プレイヤーの北側に一列。見下ろし視点で撮る） ---
			server.runOnServer(ms -> {
				net.minecraft.server.level.ServerPlayer pl = ms.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel lvl = (net.minecraft.server.level.ServerLevel) pl.level();
				String[] ids = {"rusty", "kaden", "gouen", "mimosa", "takomaru", "nita", "barley"};
				for (int i = 0; i < ids.length; i++) {
					com.amaguri.brawl.bot.BrawlBot b = com.amaguri.brawl.bot.BrawlBot.spawn(lvl,
							pl.position().add(-6 + i * 2, 0, -4), ids[i], ids[i]);
					if (b == null) continue;
					b.setNoAi(true);
					b.setYRot(0f); b.setYHeadRot(0f); b.setYBodyRot(0f);
				}
			});
			context.waitTicks(50);
			context.takeScreenshot(TestScreenshotOptions.of("lineup_lobby").disableCounterPrefix());
			server.runCommand("kill @e[type=brawl:brawl_bot,tag=!brawl_lobby]");
			context.waitTicks(5);

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
