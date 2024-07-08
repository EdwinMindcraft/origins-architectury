package io.github.edwinmindcraft.origins.api.event;

import io.github.apace100.apoli.integration.PowerLoadEvent;
import io.github.apace100.origins.badge.Badge;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * Callback which is called when a power hasn't got any badges from json and was expecting a fallback<br>
 * Badge fallbacks can be added on this callback.<br>
 * The callback is not informing whether the power is a subpower,<br>
 * as all badges from subpowers will be merged to the main power on {@link PowerLoadEvent.Post}.<br>
 * All created badges should be added to the provided list.<br>
 */
public class AutoBadgeEvent extends Event {
	private final ResourceLocation registryName;
	private final ConfiguredPower<?, ?> power;
	private final List<Badge> badges;

	public AutoBadgeEvent(ResourceLocation registryName, ConfiguredPower<?, ?> power, List<Badge> badges) {
		this.registryName = registryName;
		this.power = power;
		this.badges = badges;
	}

	public ResourceLocation getRegistryName() {
		return this.registryName;
	}

	public ConfiguredPower<?, ?> getPower() {
		return this.power;
	}

	public List<Badge> getBadges() {
		return this.badges;
	}
}
