package io.github.edwinmindcraft.origins.api.origin;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityCondition;
import io.github.edwinmindcraft.apoli.api.registry.ApoliBuiltinRegistries;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.calio.api.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public record ConditionedOrigin(
		@NotNull Holder<ConfiguredEntityCondition<?, ?>> condition,
		List<HolderSet<Origin>> origins) {

	public static final Codec<ConditionedOrigin> LARGE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ConfiguredEntityCondition.optional("condition").forGetter(ConditionedOrigin::condition),
			Origin.CODEC_SET.set().fieldOf("origins").forGetter(ConditionedOrigin::origins)
	).apply(instance, ConditionedOrigin::new));

	public static final Codec<ConditionedOrigin> STRING_CODEC = Codec.STRING.flatXmap(s -> {
		boolean tag = s.startsWith("#");
		ResourceLocation resourceLocation = ResourceLocation.tryParse(tag ? s.substring(1) : s);
		DataResult<List<HolderSet<Origin>>> origins;
		if (resourceLocation == null)
			origins = DataResult.success(ImmutableList.of());
		else if (tag)
			origins = DataResult.success(ImmutableList.of(OriginsAPI.getOriginsRegistry().getOrCreateTag(TagKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, resourceLocation))));
		else
            origins = DataResult.success(ImmutableList.of(HolderSet.direct(OriginsAPI.getOriginsRegistry().createRegistrationLookup().getOrThrow(ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, resourceLocation)))));
        return origins.map(set -> new ConditionedOrigin(ApoliBuiltinRegistries.CONFIGURED_ENTITY_CONDITIONS.get().getHolder(ApoliDynamicRegistries.CONDITION_DEFAULT).orElseThrow(), set));
	}, co -> {
		if (co.origins().size() != 1)
			return DataResult.error(() -> "Invalid size: " + co.origins().size());
		return co.origins().get(0).unwrap().<DataResult<ResourceLocation>>map(x -> DataResult.success(x.location()), x -> {
			if (x.size() != 1)
				return DataResult.error(() -> "Cannot serialize non-tag list");
			return x.get(0).unwrapKey().map(ResourceKey::location).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unregistered origin"));
		}).map(ResourceLocation::toString);
	});

	//Use the large codec since NBT Lists cannot support multiple types, safe for all operations.
	public static final Codec<ConditionedOrigin> CODEC = Codec.either(STRING_CODEC, LARGE_CODEC)
			.xmap(e -> e.map(Function.identity(), Function.identity()), Either::right);

	//Unfortunately, this is unsafe for network operations.
	private static final Codec<ConditionedOrigin> JSON_CODEC = Codec.either(STRING_CODEC, LARGE_CODEC)
			.xmap(e -> e.map(Function.identity(), Function.identity()), co -> co.origins().size() == 1 && co.condition().is(ApoliDynamicRegistries.CONDITION_DEFAULT) ? Either.left(co) : Either.right(co));


	/**
	 * Builds a stream of all origins that belong in this conditioned origins if the players
	 * matches the required condition, {@link Stream#empty()} otherwise.
	 * This will not return unbound holders.
	 */
	public Stream<Holder<Origin>> stream(Player player) {
		return ConfiguredEntityCondition.check(this.condition(), player) ? this.stream() : Stream.empty();
	}

	/**
	 * Builds a stream of all origins that belong in this conditioned origins.
	 * This will not return unbound holders.
	 */
	public Stream<Holder<Origin>> stream() {
		return this.origins().stream().flatMap(HolderSet::stream).filter(Holder::isBound);
	}

	public boolean isEmpty() {
		return this.origins().isEmpty();
	}

	public ConditionedOrigin cleanup(CalioDynamicRegistryManager registries) {
		WritableRegistry<Origin> registry = registries.get(OriginsDynamicRegistries.ORIGINS_REGISTRY);
		ImmutableList.Builder<Holder<Origin>> directBuilder = ImmutableList.builder();
		ImmutableList.Builder<HolderSet<Origin>> builder = ImmutableList.builder();
		for (HolderSet<Origin> origin : this.origins()) {
			Either<TagKey<Origin>, List<Holder<Origin>>> unwrap = origin.unwrap();
			unwrap.ifLeft(originTagKey -> {
				Optional<HolderSet.Named<Origin>> tag = registry.getTag(originTagKey);
				if (tag.isPresent() && tag.get().stream().anyMatch(Holder::isBound))
					builder.add(origin);
			});
			unwrap.ifRight(list -> list.stream().filter(Holder::isBound).forEach(directBuilder::add));
		}
		ImmutableList<Holder<Origin>> direct = directBuilder.build();
		if (direct.size() > 0)
			builder.add(HolderSet.direct(direct));
		return new ConditionedOrigin(this.condition(), builder.build());
	}

	public enum Serializer implements JsonSerializer<ConditionedOrigin>, JsonDeserializer<ConditionedOrigin> {
		INSTANCE;

		@Override
		public ConditionedOrigin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			DataResult<Pair<ConditionedOrigin, JsonElement>> result = JSON_CODEC.decode(JsonOps.INSTANCE, json);
			return result.getOrThrow(false, s -> {
				throw new JsonParseException("Expected origin in layer to be either a string or an object.");
			}).getFirst();
		}

		@Override
		public JsonElement serialize(ConditionedOrigin src, Type typeOfSrc, JsonSerializationContext context) {
			if (src.isEmpty())
				return new JsonPrimitive("<empty conditioned origin>");
			return JSON_CODEC.encodeStart(JsonOps.INSTANCE, src).getOrThrow(false, s -> {});
		}
	}
}
