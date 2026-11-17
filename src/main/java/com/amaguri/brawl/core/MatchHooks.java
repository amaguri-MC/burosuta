package com.amaguri.brawl.core;

import java.util.function.BiConsumer;

import net.minecraft.world.entity.LivingEntity;

/** W4 が init で登録する差し込み口。 */
public final class MatchHooks {
	private MatchHooks() {}

	/** (脱落した人, 倒した人 or null) */
	public static BiConsumer<LivingEntity, LivingEntity> onEliminated = (v, k) -> {};
}
