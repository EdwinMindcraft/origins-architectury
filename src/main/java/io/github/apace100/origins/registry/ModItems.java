package io.github.apace100.origins.registry;

import io.github.apace100.origins.content.OrbOfOriginItem;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ModItems {

	public static final Supplier<Item> ORB_OF_ORIGIN = OriginRegisters.ITEMS.register("orb_of_origin", OrbOfOriginItem::new);

	public static void register() {}
}
