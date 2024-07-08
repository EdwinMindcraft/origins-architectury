package io.github.edwinmindcraft.origins.api.registry;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.BadgeFactory;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class OriginsBuiltinRegistries {
	public static final ResourceKey<Registry<BadgeFactory>> BADGE_FACTORY_KEY = ResourceKey.createRegistryKey(Origins.identifier("badge_factory"));

	public static Registry<Origin> ORIGINS;
	public static Registry<BadgeFactory> BADGE_FACTORIES;
}
