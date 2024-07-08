package io.github.edwinmindcraft.origins.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.github.edwinmindcraft.calio.api.registry.DynamicEntryFactory;
import io.github.edwinmindcraft.calio.api.registry.DynamicEntryValidator;
import io.github.edwinmindcraft.calio.api.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.data.PartialOrigin;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginUpgrade;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public enum OriginLoader implements DynamicEntryValidator<Origin>, DynamicEntryFactory<Origin> {
	INSTANCE;

	private static final Comparator<PartialOrigin> LOADING_ORDER = Comparator.comparingInt(PartialOrigin::loadingOrder);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
			.registerTypeAdapter(PartialOrigin.class, PartialOrigin.Serializer.INSTANCE)
			.registerTypeAdapter(OriginUpgrade.class, CalioCodecHelper.jsonAdapter(OriginUpgrade.CODEC))
			.create();

	@Override
	public @Nullable Origin accept(@NotNull ResourceLocation id, @NotNull List<JsonElement> list) {
		Optional<Origin> origin = list.stream().flatMap(x -> {
			try {
				return Stream.of(GSON.fromJson(x, PartialOrigin.class));
			} catch (Exception e) {
				Origins.LOGGER.error("There was a problem reading Origin file " + id + " (skipping): " + e.getMessage());
				return Stream.empty();
			}
		}).max(LOADING_ORDER).map(x -> x.create(id));
		if (origin.isPresent())
			return origin.get();
		Origins.LOGGER.error("All instances of origin {} failed to load. Skipped", id);
		return null;
	}

	@Override
	public @NotNull DataResult<Origin> validate(@NotNull ResourceLocation resourceLocation, @NotNull Origin origin, @NotNull CalioDynamicRegistryManager iCalioDynamicRegistryManager) {
		Origin cleanup = origin.cleanup(iCalioDynamicRegistryManager);
		if (origin.getPowerAmount() > cleanup.getPowerAmount())
			Origins.LOGGER.error("Removed {} missing powers from {}", origin.getPowerAmount() - cleanup.getPowerAmount(), resourceLocation);
		return DataResult.success(cleanup);
	}
}
