package io.github.edwinmindcraft.origins.api.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

public record OriginUpgrade(ResourceLocation advancement, Holder<Origin> origin, String announcement) {
	public static final MapCodec<OriginUpgrade> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("condition").forGetter(OriginUpgrade::advancement),
			Origin.HOLDER_REFERENCE.fieldOf("origin").forGetter(OriginUpgrade::origin),
			ExtraCodecs.strictOptionalField(Codec.STRING, "announcement", "").forGetter(OriginUpgrade::announcement)
	).apply(instance, OriginUpgrade::new));

	public static final Codec<OriginUpgrade> CODEC = MAP_CODEC.codec();
}
