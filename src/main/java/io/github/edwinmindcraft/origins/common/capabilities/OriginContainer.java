package io.github.edwinmindcraft.origins.common.capabilities;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.component.PlayerOriginComponent;
import io.github.apace100.origins.util.ChoseOriginCriterion;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.IOriginCallbackPower;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2CSynchronizeOrigin;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class OriginContainer implements IOriginContainer, ICapabilitySerializable<Tag> {

	public static final ResourceLocation ID = Origins.identifier("origins");

	private final Player player;
	private final Map<OriginLayer, Origin> layers;
	private final AtomicBoolean synchronization;
	private final AtomicBoolean hadAllOrigins;
	private boolean cleanupPowers = true;

	public OriginContainer(Player player) {
		this.player = player;
		this.layers = new ConcurrentHashMap<>();
		this.synchronization = new AtomicBoolean();
		this.hadAllOrigins = new AtomicBoolean();
	}

	@Override
	public void setOrigin(@NotNull OriginLayer layer, @NotNull Origin origin) {
		setOriginInternal(layer, origin, true);
	}

	public void setOriginInternal(@NotNull OriginLayer layer, @NotNull Origin origin, boolean handlePowers) {
		Origin previous = this.layers.put(layer, origin);
		if (!Objects.equals(origin, previous) || !handlePowers) {
			if (handlePowers) {
				IPowerContainer.get(this.player).ifPresent(container -> {
					this.grantPowers(container, origin);
					if (previous != null)
						container.removeAllPowersFromSource(OriginsAPI.getPowerSource(previous));
					if (this.hasAllOrigins())
						this.hadAllOrigins.set(true);
					if (this.player instanceof ServerPlayer sp)
						ChoseOriginCriterion.INSTANCE.trigger(sp, origin);
				});
			}
			this.synchronize();
		}
	}

	private void grantPowers(IPowerContainer container, Origin origin) {
		ResourceLocation powerSource = OriginsAPI.getPowerSource(origin);
		Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers();
		for (ResourceLocation power : origin.getPowers()) {
			ConfiguredPower<?, ?> configuredPower = powers.get(power);
			if (configuredPower != null && !container.hasPower(power, powerSource))
				container.addPower(power, powerSource);
		}
	}

	@Override
	public @NotNull Origin getOrigin(@NotNull OriginLayer layer) {
		return this.layers.getOrDefault(layer, Origin.EMPTY);
	}

	@Override
	public boolean hasOrigin(@NotNull OriginLayer layer) {
		return !Objects.equals(this.getOrigin(layer), OriginRegisters.EMPTY.get());
	}

	@Override
	public boolean hadAllOrigins() {
		return this.hadAllOrigins.get();
	}

	@Override
	public @NotNull Map<OriginLayer, Origin> getOrigins() {
		return ImmutableMap.copyOf(this.layers);
	}

	@Override
	public void synchronize() {
		this.synchronization.compareAndSet(false, true);
	}

	@Override
	public boolean shouldSync() {
		return this.synchronization.get();
	}

	@Override
	public @NotNull Player getOwner() {
		return this.player;
	}

	@Override
	public void tick() {
		if (this.cleanupPowers) {
			this.cleanupPowers = false;
			IPowerContainer.get(this.player).ifPresent(container -> {
				for (Origin origin : this.layers.values()) {
					ResourceLocation powerSource = OriginsAPI.getPowerSource(origin);
					Set<ResourceLocation> currentPowers = ImmutableSet.copyOf(container.getPowersFromSource(powerSource));
					Registry<ConfiguredPower<?, ?>> registry = ApoliAPI.getPowers(this.player.getServer());

					Set<ResourceLocation> newPowers = origin.getPowers().stream().map(registry::get).filter(Objects::nonNull).flatMap(x -> {
						HashSet<ResourceLocation> names = new HashSet<>();
						names.add(x.getRegistryName());
						x.getChildren().stream().filter(Holder::isBound).map(Holder::value).map(ConfiguredPower::getRegistryName).forEach(names::add);
						return names.stream();
					}).collect(ImmutableSet.toImmutableSet());
					Set<ResourceLocation> toRemove = currentPowers.stream().filter(x -> !newPowers.contains(x)).collect(Collectors.toSet());
					Set<ResourceLocation> toAdd = newPowers.stream().filter(x -> !currentPowers.contains(x)).collect(Collectors.toSet());
					if (!toRemove.isEmpty()) {
						toRemove.forEach(power -> container.removePower(power, powerSource));
						Origins.LOGGER.debug("CLEANUP: Revoked {} removed powers for origin {} on player {}", toRemove.size(), origin.getRegistryName(), this.player.getScoreboardName());
					}
					if (!toAdd.isEmpty()) {
						toAdd.forEach(power -> container.addPower(power, powerSource));
						Origins.LOGGER.debug("CLEANUP: Granted {} missing powers for origin {} on player {}", toAdd.size(), origin.getRegistryName(), this.player.getScoreboardName());
					}
				}
			});
		}
		if (this.shouldSync() && !this.player.level.isClientSide() && this.syncCooldown.decrementAndGet() <= 0) {
			OriginsCommon.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this.player), this.getSynchronizationPacket());
			this.syncCooldown.set(20);
			ApoliAPI.synchronizePowerContainer(this.player);
		}
	}

	@NotNull
	@Override
	public S2CSynchronizeOrigin getSynchronizationPacket() {
		return new S2CSynchronizeOrigin(this.player.getId(), this.getLayerMap(), this.hadAllOrigins());
	}

	@NotNull
	private Map<ResourceLocation, ResourceLocation> getLayerMap() {
		ImmutableMap.Builder<ResourceLocation, ResourceLocation> builder = ImmutableMap.builder();
		Registry<OriginLayer> layers = OriginsAPI.getLayersRegistry();
		this.layers.forEach((layer, origin) -> {
			ResourceLocation key = layers.getKey(layer);
			ResourceLocation value = origin.getRegistryName();
			if (key != null && value != null)
				builder.put(key, value);
		});
		return builder.build();
	}

	@Override
	public boolean checkAutoChoosingLayers(boolean includeDefaults) {
		boolean choseOneAutomatically = false;
		Registry<Origin> registry = OriginsAPI.getOriginsRegistry();
		for (OriginLayer layer : OriginsAPI.getActiveLayers()) {
			boolean shouldContinue = false;
			if (!this.hasOrigin(layer)) {
				Origin def;
				if (includeDefaults && layer.hasDefaultOrigin() && (def = registry.get(layer.defaultOrigin())) != null) {
					this.setOrigin(layer, def);
					choseOneAutomatically = true;
					shouldContinue = true;
				} else {
					Optional<Origin> automaticOrigin = layer.getAutomaticOrigin(this.player);
					if (automaticOrigin.isPresent()) {
						this.setOrigin(layer, automaticOrigin.get());
						choseOneAutomatically = true;
						shouldContinue = true;
					} else if (layer.getOriginOptionCount(this.player) == 0)
						shouldContinue = true;
				}
			} else
				shouldContinue = true;
			if (!shouldContinue)
				break;
		}
		return choseOneAutomatically;
	}

	@Override
	public void onChosen(@NotNull Origin origin, boolean isOrb) {
		IPowerContainer.get(this.player).ifPresent(container -> container.getPowersFromSource(OriginsAPI.getPowerSource(origin)).stream()
				.map(container::getPower)
				.filter(Objects::nonNull)
				.forEach((ConfiguredPower<?, ?> power) -> IOriginCallbackPower.onChosen(power, this.player, isOrb)));
	}

	@Override
	public void onChosen(boolean isOrb) {
		IPowerContainer.get(this.player).ifPresent(container -> container.getPowers().forEach(x -> IOriginCallbackPower.onChosen(x, this.player, isOrb)));
	}

	@Override
	public void onReload() {
		this.cleanupPowers = true;
	}

	private final Lazy<OriginComponent> component = Lazy.of(() -> new PlayerOriginComponent(this));

	@Override
	public @NotNull OriginComponent asLegacyComponent() {
		return this.component.get();
	}

	private final LazyOptional<IOriginContainer> thisOptional = LazyOptional.of(() -> this);

	@NotNull
	@Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return OriginsAPI.ORIGIN_CONTAINER.orEmpty(cap, this.thisOptional);
	}

	public void acceptSynchronization(Map<ResourceLocation, ResourceLocation> map, boolean hadAllOrigins) {
		this.layers.clear();
		Registry<OriginLayer> layers = OriginsAPI.getLayersRegistry();
		Registry<Origin> origins = OriginsAPI.getOriginsRegistry();
		map.forEach((layer, origin) -> layers.getOptional(layer).ifPresent(l -> origins.getOptional(origin).ifPresent(o -> this.layers.put(l, o))));
		this.hadAllOrigins.set(hadAllOrigins);
	}

	@Override
	public Tag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		CompoundTag layers = new CompoundTag();
		this.getOrigins().forEach((layer, origin) -> {
			ResourceLocation key = layer.getRegistryName();
			ResourceLocation value = origin.getRegistryName();
			if (key == null || value == null) {
				Origins.LOGGER.warn("Removed missing entry {} ({}): {} ({})", layer, key, origin, value);
				return;
			}
			layers.putString(key.toString(), value.toString());
		});
		tag.put("Origins", layers);
		tag.putBoolean("HadAllOrigins", this.hasAllOrigins());
		return tag;
	}

	@Override
	public void deserializeNBT(Tag nbt) {
		this.layers.clear();
		CompoundTag tag = (CompoundTag) nbt;
		CompoundTag layers = tag.getCompound("Origins");
		Registry<OriginLayer> registry = OriginsAPI.getLayersRegistry();
		Registry<Origin> origins = OriginsAPI.getOriginsRegistry();
		for (String key : layers.getAllKeys()) {
			String origin = layers.getString(key);
			if (origin.isBlank())
				continue;
			ResourceLocation orig = ResourceLocation.tryParse(origin);
			if (orig == null) {
				Origins.LOGGER.warn("Invalid origin {} found for layer {} on entity {}", origin, key, this.player.getScoreboardName());
				continue;
			}
			Origin origin1 = origins.get(orig);
			if (origin1 == null) {
				Origins.LOGGER.warn("Missing origin {} found for layer {} on entity {}", origin, key, this.player.getScoreboardName());
				IPowerContainer.get(this.player).ifPresent(container -> container.removeAllPowersFromSource(OriginsAPI.getPowerSource(orig)));
				continue;
			}
			ResourceLocation rl = ResourceLocation.tryParse(key);
			if (rl == null) {
				Origins.LOGGER.warn("Invalid layer found {} on entity {}", key, this.player.getScoreboardName());
				IPowerContainer.get(this.player).ifPresent(container -> container.removeAllPowersFromSource(OriginsAPI.getPowerSource(origin1)));
				continue;
			}
			OriginLayer layer = registry.get(rl);
			if (layer == null) {
				Origins.LOGGER.warn("Missing layer {} on entity {}", rl, this.player.getScoreboardName());
				IPowerContainer.get(this.player).ifPresent(container -> container.removeAllPowersFromSource(OriginsAPI.getPowerSource(origin1)));
				continue;
			}
			this.setOriginInternal(layer, origin1, false);
		}
		this.hadAllOrigins.set(tag.getBoolean("HadAllOrigins"));
	}

	private final AtomicInteger syncCooldown = new AtomicInteger(0);
	@Override
	public void validateSynchronization() {
		this.synchronization.compareAndSet(true, false);
		this.syncCooldown.set(0);
	}
}
