package com.amaguri.brawl.client;

import net.fabricmc.api.ClientModInitializer;

/** クライアント入口（司令塔所有）。 */
public class BrawlClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		com.amaguri.brawl.client.hud.HudClient.init();       // W6 HUD・入力・茂み非表示・HPバー
		com.amaguri.brawl.client.bot.BotClient.init();       // W5 ボットの描画
	}
}
