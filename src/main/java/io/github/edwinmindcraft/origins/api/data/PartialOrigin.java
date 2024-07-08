package io.github.edwinmindcraft.origins.api.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.apace100.origins.data.CompatibilityDataTypes;
import io.github.apace100.origins.origin.Impact;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginUpgrade;
import io.github.edwinmindcraft.origins.api.util.JsonUtils;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public record PartialOrigin(@NotNull List<HolderSet<ConfiguredPower<?, ?>>> powers,
							@Nullable ItemStack icon, @Nullable Boolean unchoosable,
							@Nullable Integer order,
							@Nullable Impact impact, @Nullable String name,
							@Nullable String description,
							@NotNull Set<OriginUpgrade> upgrades,
							int loadingOrder) {

	public Origin create(ResourceLocation name) {
		return new Origin(
				ImmutableSet.copyOf(this.powers()),
				this.icon() != null ? this.icon() : ItemStack.EMPTY,
				this.unchoosable() != null ? this.unchoosable() : false,
				this.order() != null ? this.order() : Integer.MAX_VALUE,
				this.impact() != null ? this.impact() : Impact.NONE,
				Component.translatable(this.name() != null ? this.name() : "origin." + name.getNamespace() + "." + name.getPath() + ".name"),
				Component.translatable(this.description() != null ? this.description() : "origin." + name.getNamespace() + "." + name.getPath() + ".description"),
				ImmutableSet.copyOf(this.upgrades()));
	}

	public enum Serializer implements JsonSerializer<PartialOrigin>, JsonDeserializer<PartialOrigin> {
		INSTANCE;

		@Override
		public PartialOrigin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject root = GsonHelper.convertToJsonObject(json, "root element");
			Builder builder = builder();
			if (root.has("powers"))
				builder.powers(root.getAsJsonArray("powers"));
			if (root.has("icon")) {
				JsonElement icon = root.get("icon");
				ItemStack read = CompatibilityDataTypes.ITEM_OR_ITEM_STACK.read(icon);
				if (read != null) builder.icon(read);
			}
			JsonUtils.getOptional(root, "unchoosable", GsonHelper::getAsBoolean).ifPresent(builder::unchoosable);
			JsonUtils.getOptional(root, "order", GsonHelper::getAsInt).ifPresent(builder::order);
			JsonUtils.getOptional(root, "impact", GsonHelper::getAsInt).ifPresent(x -> {
				if (x < 0 || x >= Impact.values().length)
					throw new JsonParseException("Impact must be between 0 and " + (Impact.values().length - 1) + ", was " + x);
				builder.impact(Impact.values()[x]);
			});
			JsonUtils.getOptional(root, "name", GsonHelper::getAsString).ifPresent(builder::name);
			JsonUtils.getOptional(root, "description", GsonHelper::getAsString).ifPresent(builder::description);
			builder.upgrades(JsonUtils.getOptionalList(root, "upgrades", (x, s) -> context.deserialize(x, OriginUpgrade.class)));
			JsonUtils.getOptional(root, "loading_priority", GsonHelper::getAsInt).ifPresent(builder::loadingOrder);
			return builder.build();
		}

		@Override
		public JsonElement serialize(PartialOrigin src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject root = new JsonObject();
			DataResult<JsonElement> powers = ConfiguredPower.REFERENCE_SET_CODEC.listOf().encodeStart(JsonOps.INSTANCE, src.powers());
			if (powers.result().isEmpty())
				throw new IllegalArgumentException("Failed to generate power list: " + powers.error().orElseThrow().message());
			root.add("powers", powers.result().get());
			if (src.icon() != null) root.add("icon", CompatibilityDataTypes.ITEM_OR_ITEM_STACK.write(src.icon()));
			if (src.unchoosable() != null) root.addProperty("unchoosable", src.unchoosable());
			if (src.order() != null) root.addProperty("order", src.order());
			if (src.impact() != null) root.addProperty("impact", src.impact().ordinal());
			if (src.name() != null) root.addProperty("name", src.name());
			if (src.description() != null) root.addProperty("description", src.description());
			root.add("upgrades", src.upgrades().stream().map(x -> context.serialize(x, OriginUpgrade.class)).collect(JsonUtils.toJsonArray()));
			if (src.loadingOrder() != 0) root.addProperty("loading_priority", src.loadingOrder());
			return root;
		}
	}

	public static Builder builder() {return new Builder();}

	public static final class Builder {

		private final ImmutableList.Builder<HolderSet<ConfiguredPower<?, ?>>> powers = ImmutableList.builder();
		private final ImmutableSet.Builder<OriginUpgrade> upgrades = ImmutableSet.builder();

		private ItemStack icon;
		private Boolean unchoosable;
		private Integer order;
		private Impact impact;
		private String name;
		private String description;
		private int loadingOrder = 0;

		private Builder() {}

		@Contract("_ -> this")
		public Builder powers(JsonArray powers) throws JsonParseException {
			DataResult<Pair<List<HolderSet<ConfiguredPower<?, ?>>>, JsonElement>> decode = ConfiguredPower.REFERENCE_SET_CODEC.listOf().decode(JsonOps.INSTANCE, powers);
			if (decode.result().isEmpty())
				throw new JsonParseException("Failed to deserialize powers: " + decode.error().orElseThrow().message());
			this.powers.addAll(decode.result().get().getFirst());
			return this;
		}

		@Contract("_ -> this")
		public Builder powers(Iterable<ResourceLocation> powers) {
			Registry<ConfiguredPower<?, ?>> registry = ApoliAPI.getPowers(ServerLifecycleHooks.getCurrentServer() != null ? ServerLifecycleHooks.getCurrentServer().registryAccess() : RegistryAccess.EMPTY);
			List<ResourceKey<ConfiguredPower<?, ?>>> keys = Streams.stream(powers).map(loc -> ResourceKey.create(ApoliDynamicRegistries.CONFIGURED_POWER_KEY, loc)).toList();
			this.powers.add(HolderSet.direct(registry::getHolderOrThrow, keys));
			return this;
		}

		@Contract("_ -> this")
		public Builder powers(ResourceLocation... powers) {
			return this.powers(Arrays.asList(powers));
		}

		@Contract("_ -> this")
		public Builder icon(@Nullable ItemStack icon) {
			this.icon = icon;
			return this;
		}

		@Contract("_ -> this")
		public Builder unchoosable(@Nullable Boolean unchoosable) {
			this.unchoosable = unchoosable;
			return this;
		}

		@Contract("_ -> this")
		public Builder order(@Nullable Integer order) {
			this.order = order;
			return this;
		}

		@Contract("_ -> this")
		public Builder impact(@Nullable Impact impact) {
			this.impact = impact;
			return this;
		}

		@Contract("_ -> this")
		public Builder name(@Nullable String name) {
			this.name = name;
			return this;
		}

		@Contract("_ -> this")
		public Builder description(@Nullable String description) {
			this.description = description;
			return this;
		}

		@Contract("_ -> this")
		public Builder upgrades(Iterable<OriginUpgrade> upgrades) {
			this.upgrades.addAll(upgrades);
			return this;
		}

		@Contract("_ -> this")
		public Builder upgrades(@Nullable OriginUpgrade... upgrades) {
			this.upgrades.add(upgrades);
			return this;
		}

		@Contract("_ -> this")
		public Builder loadingOrder(int loadingOrder) {
			this.loadingOrder = loadingOrder;
			return this;
		}

		public PartialOrigin build() {return new PartialOrigin(this.powers.build(), this.icon, this.unchoosable, this.order, this.impact, this.name, this.description, this.upgrades.build(), this.loadingOrder);}
	}
}
