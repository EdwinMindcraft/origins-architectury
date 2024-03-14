package io.github.edwinmindcraft.origins.common.condition.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record OriginConfiguration(Holder<Origin> origin,
								  @Nullable Holder<OriginLayer> layer) implements IDynamicFeatureConfiguration {
	public static final Codec<OriginConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Origin.HOLDER_REFERENCE.fieldOf("origin").forGetter(OriginConfiguration::origin),
			ExtraCodecs.strictOptionalField(OriginLayer.HOLDER_REFERENCE, "layer").forGetter(x -> Optional.ofNullable(x.layer()))
	).apply(instance, (o, l) -> new OriginConfiguration(o, l.orElse(null))));
}
