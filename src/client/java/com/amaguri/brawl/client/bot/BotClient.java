package com.amaguri.brawl.client.bot;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.amaguri.brawl.bot.BotEntities;

/** client 側。ボットをプレイヤーモデル＋既定スキンで描く。 */
public final class BotClient {
	private BotClient() {
	}

	public static void init() {
		EntityRendererRegistry.register(BotEntities.BRAWL_BOT, BotRenderer::new);
	}
}
