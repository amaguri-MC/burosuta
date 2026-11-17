package com.amaguri.brawl.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.amaguri.brawl.BrawlMod;

/** W3: アリーナのブロックと POWER_CUBE アイテム。 */
public final class ArenaBlocks {
	public static Block WALL, BORDER, BUSH, BOX, ROPE, FLOOR_A, FLOOR_B;
	public static Item POWER_CUBE;

	private ArenaBlocks() {}

	/** 茂み: 通れる・光を通す・視線もパーティクルも弾も止めない（見た目は葉のcutout。レイヤーはテクスチャの透明で決まる）。 */
	private static final class BushBlock extends Block {
		BushBlock(BlockBehaviour.Properties p) {
			super(p);
		}

		@Override
		protected boolean propagatesSkylightDown(BlockState state) {
			return true;
		}

		@Override
		protected int getLightDampening(BlockState state) {
			return 0;
		}

		@Override
		protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
			return Shapes.block(); // 視線/選択用（当たり判定は noCollision で無い）
		}
	}

	/** ロープ柵: 当たり判定は高さ1.5ブロック（通れない）。見た目は細い杭とロープ。 */
	private static final class RopeBlock extends Block {
		private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 24, 16);
		private static final VoxelShape OUTLINE = Block.box(5, 0, 5, 11, 14, 11);

		RopeBlock(BlockBehaviour.Properties p) {
			super(p);
		}

		@Override
		protected VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
			return COLLISION;
		}

		@Override
		protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
			return OUTLINE;
		}

		@Override
		protected VoxelShape getOcclusionShape(BlockState s) {
			return Shapes.empty();
		}

		@Override
		protected boolean propagatesSkylightDown(BlockState state) {
			return true;
		}

		@Override
		protected int getLightDampening(BlockState state) {
			return 0;
		}
	}

	public static void init() {
		WALL = block("wall", new Block(props("wall").mapColor(MapColor.SAND).strength(2.0f).sound(SoundType.STONE)));
		BORDER = block("border", new Block(props("border").mapColor(MapColor.COLOR_GRAY).strength(-1.0f, 3600000.0f).sound(SoundType.STONE)));
		BUSH = block("bush", new BushBlock(props("bush").mapColor(MapColor.PLANT).noCollision().noOcclusion()
				.strength(0.2f).sound(SoundType.GRASS)
				.isSuffocating((s, g, p) -> false).isViewBlocking((s, g, p) -> false)));
		BOX = block("box", new Block(props("box").mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).lightLevel(s -> 6)));
		ROPE = block("rope", new RopeBlock(props("rope").mapColor(MapColor.WOOD).noOcclusion().strength(1.0f).sound(SoundType.WOOD)
				.isSuffocating((s, g, p) -> false).isViewBlocking((s, g, p) -> false)));
		FLOOR_A = block("floor_a", new Block(props("floor_a").mapColor(MapColor.GRASS).strength(2.0f).sound(SoundType.GRASS)));
		FLOOR_B = block("floor_b", new Block(props("floor_b").mapColor(MapColor.GRASS).strength(2.0f).sound(SoundType.GRASS)));

		Identifier cubeId = id("power_cube");
		POWER_CUBE = Registry.register(BuiltInRegistries.ITEM, cubeId,
				new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, cubeId))));
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(BrawlMod.MODID, name);
	}

	private static BlockBehaviour.Properties props(String name) {
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name))).noLootTable();
	}

	/** ブロックを登録し、同名の BlockItem も登録する（デバッグ用に /give できる）。 */
	private static Block block(String name, Block b) {
		Identifier id = id(name);
		Registry.register(BuiltInRegistries.BLOCK, id, b);
		Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(b,
				new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
		return b;
	}
}
