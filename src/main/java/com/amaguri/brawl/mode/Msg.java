package com.amaguri.brawl.mode;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** 表示まわりの小道具。文言は t("キー", "日本語") の形で書く（agent/lang/W4.json に同じ組を置く）。 */
final class Msg {
	private Msg() {}

	static MutableComponent t(String key, String fallback, Object... args) {
		return Component.translatableWithFallback(key, fallback, args);
	}

	static void title(ServerPlayer p, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
		if (true) return; // 大きな文字はクライアントの本家風UIが出す（バニラのタイトルと重なるので送らない）
		p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle == null ? Component.empty() : subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	static void actionBar(ServerPlayer p, Component c) {
		p.sendSystemMessage(c, true);
	}

	static void chat(ServerPlayer p, Component c) {
		p.sendSystemMessage(c, false);
	}
}
