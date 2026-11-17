package com.amaguri.brawl.client.model;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.amaguri.brawl.api.Net;

/** キャラモデル（tools/MODEL_FORMAT.md）をプレイヤーとボットに着せる client 側の入口。BrawlClient から init() を呼ぶ。 */
public final class ModelClient {
	private ModelClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Net.LooksPayload.TYPE, (p, ctx) -> BrawlerLooks.set(p.ids(), p.brawlers()));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> BrawlerLooks.clear());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> BrawlerLooks.clear());
	}
}
