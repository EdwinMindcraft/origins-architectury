package io.github.edwinmindcraft.origins.common.power.configuration;

import com.mojang.serialization.Codec;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;

public record WaterVisionConfiguration(float strength) implements IDynamicFeatureConfiguration {
	public static final Codec<WaterVisionConfiguration> CODEC = ExtraCodecs.strictOptionalField(CalioCodecHelper.FLOAT, "strength", 1.0F).xmap(WaterVisionConfiguration::new, WaterVisionConfiguration::strength).codec();
}
