package com.amaguri.brawl;

import net.fabricmc.api.ModInitializer;

import com.amaguri.brawl.api.Net;

/** 入口（司令塔所有）。各担当の init をこの順で呼ぶ。 */
public class BrawlMod implements ModInitializer {
	public static final String MODID = "brawl";

	@Override
	public void onInitialize() {
		Net.registerTypes();
		com.amaguri.brawl.arena.ArenaBlocks.init();      // W3 ブロック・アイテム登録
		com.amaguri.brawl.core.Combat.init();            // W1 tick・同期・ダメージ無効化
		com.amaguri.brawl.brawler.Brawlers.init();       // W2 キャラ定義・武器アイテム・C2S受信
		com.amaguri.brawl.bot.BotEntities.init();        // W5 ボット実体
		com.amaguri.brawl.mode.Showdown.init();          // W4 試合・コマンド
	}
}
