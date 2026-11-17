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
			c.options.renderDistance().set(10);
			// UNVERIFIED: 26.2 の Options に hideGui フィールドが apidump に無い。既定で HUD は出る（隠さない）
		});
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksDownload();
			TestServerContext server = sp.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");

			server.runCommand("brawl start 9");
			// 3秒カウントダウンが明けてから 3秒後
			context.waitTicks(COUNTDOWN_TICKS + 60);
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot(TestScreenshotOptions.of("brawl_hud_3s").disableCounterPrefix());

			// 10秒後
			context.waitTicks(140);
			context.takeScreenshot(TestScreenshotOptions.of("brawl_hud_10s").disableCounterPrefix());

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
		}
	}
}
