package io.github.edwinmindcraft.origins.common.power.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;

public record WaterVisionConfiguration(float strength) implements IDynamicFeatureConfiguration {
	public static final MapCodec<WaterVisionConfiguration> CODEC = CalioCodecHelper.FLOAT.optionalFieldOf("strength", 1.0F).xmap(WaterVisionConfiguration::new, WaterVisionConfiguration::strength);
}
