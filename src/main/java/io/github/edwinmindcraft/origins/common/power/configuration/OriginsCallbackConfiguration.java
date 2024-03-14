package io.github.edwinmindcraft.origins.common.power.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;

public record OriginsCallbackConfiguration(Holder<ConfiguredEntityAction<?, ?>> entityActionRespawned,
										   Holder<ConfiguredEntityAction<?, ?>> entityActionRemoved,
										   Holder<ConfiguredEntityAction<?, ?>> entityActionGained,
										   Holder<ConfiguredEntityAction<?, ?>> entityActionLost,
										   Holder<ConfiguredEntityAction<?, ?>> entityActionAdded,
										   Holder<ConfiguredEntityAction<?, ?>> entityActionChosen,
										   boolean onOrb) implements IDynamicFeatureConfiguration {
	public static final Codec<OriginsCallbackConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ConfiguredEntityAction.optional("entity_action_respawned").forGetter(OriginsCallbackConfiguration::entityActionRespawned),
			ConfiguredEntityAction.optional("entity_action_removed").forGetter(OriginsCallbackConfiguration::entityActionRemoved),
			ConfiguredEntityAction.optional("entity_action_gained").forGetter(OriginsCallbackConfiguration::entityActionGained),
			ConfiguredEntityAction.optional("entity_action_lost").forGetter(OriginsCallbackConfiguration::entityActionLost),
			ConfiguredEntityAction.optional("entity_action_added").forGetter(OriginsCallbackConfiguration::entityActionAdded),
			ConfiguredEntityAction.optional("entity_action_chosen").forGetter(OriginsCallbackConfiguration::entityActionChosen),
			ExtraCodecs.strictOptionalField(CalioCodecHelper.BOOL, "execute_chosen_when_orb", true).forGetter(OriginsCallbackConfiguration::onOrb)
	).apply(instance, OriginsCallbackConfiguration::new));
}
