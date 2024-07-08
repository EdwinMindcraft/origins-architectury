package io.github.apace100.origins.registry;

import io.github.apace100.origins.content.TemporaryCobwebBlock;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class ModBlocks {

	public static final Supplier<TemporaryCobwebBlock> TEMPORARY_COBWEB = register("temporary_cobweb", () -> new TemporaryCobwebBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).forceSolidOn().noCollission().requiresCorrectToolForDrops().strength(4.0F)), false);

	public static void register() {
	}

	private static <T extends Block> Supplier<T> register(String blockName, Supplier<T> block) {
		return register(blockName, block, true);
	}

	private static <T extends Block> Supplier<T> register(String blockName, Supplier<T> block, boolean withBlockItem) {
		Supplier<T> register = OriginRegisters.BLOCKS.register(blockName, block);
		if (withBlockItem)
			OriginRegisters.ITEMS.register(blockName, () -> new BlockItem(register.get(), new Item.Properties()));
		return register;
	}
}