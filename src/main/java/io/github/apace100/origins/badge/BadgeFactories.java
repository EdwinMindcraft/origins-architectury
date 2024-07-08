package io.github.apace100.origins.badge;

import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;

import java.util.function.Function;
import java.util.function.Supplier;

public final class BadgeFactories {

	public static final Supplier<BadgeFactory> SPRITE = register("sprite",
			new SerializableData()
					.add("sprite", SerializableDataTypes.IDENTIFIER),
			SpriteBadge::new);

	public static final Supplier<BadgeFactory> TOOLTIP = register("tooltip",
			new SerializableData()
					.add("sprite", SerializableDataTypes.IDENTIFIER)
					.add("text", SerializableDataTypes.TEXT),
			TooltipBadge::new);

	// Added mostly for backwards-compatibility as the default factory.
	public static final Supplier<BadgeFactory> KEYBIND = register("keybind",
			new SerializableData()
					.add("sprite", SerializableDataTypes.IDENTIFIER)
					.add("text", SerializableDataTypes.STRING),
			KeybindBadge::new);

	public static final Supplier<BadgeFactory> CRAFTING_RECIPE = register("crafting_recipe",
			new SerializableData()
					.add("sprite", SerializableDataTypes.IDENTIFIER)
					.add("recipe", SerializableDataTypes.RECIPE)
					.add("prefix", SerializableDataTypes.TEXT, null)
					.add("suffix", SerializableDataTypes.TEXT, null),
			CraftingRecipeBadge::new);

	public static void bootstrap() {}

	private static Supplier<BadgeFactory> register(String name, SerializableData data, Function<SerializableData.Instance, Badge> factory) {
		return OriginRegisters.BADGE_FACTORIES.register(name, () -> new BadgeFactory(Origins.identifier(name), data, factory));
	}
}
