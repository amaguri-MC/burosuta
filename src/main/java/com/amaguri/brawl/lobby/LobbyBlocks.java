package com.amaguri.brawl.lobby;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.amaguri.brawl.BrawlMod;

/** ロビー広場用のブロック（名前は lobby_ で始まる）。登録は {@link Lobby#init()} から1回。 */
public final class LobbyBlocks {
	private LobbyBlocks() {}

	public static Block TILE_A, TILE_B, EDGE, PAD, PEDESTAL;
	private static boolean inited;

	public static void init() {
		if (inited) return;
		inited = true;
		TILE_A = block("lobby_tile_a", new Block(props("lobby_tile_a").mapColor(MapColor.SAND).strength(-1.0f, 3600000.0f).sound(SoundType.STONE)));
		TILE_B = block("lobby_tile_b", new Block(props("lobby_tile_b").mapColor(MapColor.ICE).strength(-1.0f, 3600000.0f).sound(SoundType.STONE)));
		EDGE = block("lobby_edge", new Block(props("lobby_edge").mapColor(MapColor.COLOR_ORANGE).strength(-1.0f, 3600000.0f).sound(SoundType.STONE)));
		// 光る床（夜でも見える）
		PAD = block("lobby_pad", new Block(props("lobby_pad").mapColor(MapColor.GOLD).strength(-1.0f, 3600000.0f).sound(SoundType.STONE).lightLevel(s -> 15)));
		// 像の台座・掲示板の裏板。ほんのり光らせて夜の湧き潰しにもする
		PEDESTAL = block("lobby_pedestal", new Block(props("lobby_pedestal").mapColor(MapColor.COLOR_PURPLE).strength(-1.0f, 3600000.0f).sound(SoundType.STONE).lightLevel(s -> 8)));
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(BrawlMod.MODID, name);
	}

	private static BlockBehaviour.Properties props(String name) {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name))).noLootTable();
	}

	/** ArenaBlocks.block と同じ書き方（ブロック＋同名の BlockItem）。 */
	private static Block block(String name, Block b) {
		Identifier id = id(name);
		Registry.register(BuiltInRegistries.BLOCK, id, b);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(b,
				new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
		return b;
	}
}
