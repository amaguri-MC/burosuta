package com.amaguri.brawl.bot;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class BotEntities {
	public static EntityType<BrawlBot> BRAWL_BOT;

	private BotEntities() {
	}

	public static void init() {
		Identifier id = Identifier.fromNamespaceAndPath("brawl", "bot");
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		// MISC: 自然スポーンしない。プレイヤーと同じ当たり判定（0.6 x 1.8、目の高さ 1.62）。
		BRAWL_BOT = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.<BrawlBot>of(BrawlBot::new, MobCategory.MISC)
						.sized(0.6f, 1.8f)
						.eyeHeight(1.62f)
						.clientTrackingRange(10)
						.build(key));
		FabricDefaultAttributeRegistry.register(BRAWL_BOT, BrawlBot.createAttributes());
	}
}
