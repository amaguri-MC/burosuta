package com.amaguri.brawl.client.bot;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** ボット専用の描画状態。EntityRenderDispatcherMixin が、この型のときだけ BotRenderer に回す。 */
public class BotRenderState extends AvatarRenderState {
}
