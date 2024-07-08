package io.github.apace100.origins.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.entity.EnderianPearlEntity;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class ModEntities {

	public static final Supplier<EntityType<EnderianPearlEntity>> ENDERIAN_PEARL = register("enderian_pearl", () -> EntityType.Builder.<EnderianPearlEntity>of(EnderianPearlEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

	private static <T extends Entity> Supplier<EntityType<T>> register(String name, Supplier<EntityType.Builder<T>> builder) {
		return OriginRegisters.ENTITY_TYPES.register(name, () -> builder.get().build(Origins.MODID + ":" + name));
	}

	public static void register() {}
}
