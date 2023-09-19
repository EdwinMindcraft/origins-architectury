package io.github.edwinmindcraft.origins.common.capabilities;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.component.PlayerOriginComponent;
import io.github.apace100.origins.util.ChoseOriginCriterion;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.IOriginCallbackPower;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenWaitingForPowersScreen;
import io.github.edwinmindcraft.origins.common.network.S2CSynchronizeOrigin;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
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
import java.util.stream.Stream;

public class OriginContainer implements IOriginContainer, ICapabilitySerializable<Tag> {

	public static final ResourceLocation ID = Origins.identifier("origins");

	private final Player player;
	private final Map<ResourceKey<OriginLayer>, ResourceKey<Origin>> layers;
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
	public void setOrigin(@NotNull ResourceKey<OriginLayer> layer, @NotNull ResourceKey<Origin> origin) {
        setOriginInternal(layer, origin, true);
	}

    public void setOriginInternal(@NotNull ResourceKey<OriginLayer> layer, @NotNull ResourceKey<Origin> origin, boolean handlePowers) {
        Optional<Holder<OriginLayer>> layerHolder = OriginsAPI.getLayersRegistry().getHolder(layer);
        Optional<Holder<Origin>> originHolder = OriginsAPI.getOriginsRegistry().getHolder(origin);
        if (layerHolder.isEmpty() || !layerHolder.get().isBound()) {
            Origins.LOGGER.error("Tried to assign missing layer {} to player {}", layer, this.player.getScoreboardName());
            return;
        }
        if (originHolder.isEmpty() || !originHolder.get().isBound()) {
            Origins.LOGGER.error("Tried to assign missing origin {} to player {}", origin, this.player.getScoreboardName());
            return;
        }
        ResourceKey<Origin> previous = this.layers.put(layer, origin);
        if (!Objects.equals(origin, previous) || !handlePowers) {
            if (handlePowers) {
                IPowerContainer.get(this.player).ifPresent(container -> {
                    this.grantPowers(container, origin, originHolder.get());
                    if (previous != null)
                        container.removeAllPowersFromSource(OriginsAPI.getPowerSource(previous));
                });
                if (this.hasAllOrigins())
                    this.hadAllOrigins.set(true);
                if (this.player instanceof ServerPlayer sp)
                    ChoseOriginCriterion.INSTANCE.trigger(sp, origin);
            }
            this.synchronize();
        }
    }

	private void grantPowers(IPowerContainer container, @NotNull ResourceKey<Origin> origin, Holder<Origin> holder) {
		ResourceLocation powerSource = OriginsAPI.getPowerSource(origin);
		Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers(this.player.getServer());
		for (HolderSet<ConfiguredPower<?, ?>> holderSet : holder.value().getPowers()) {
			for (Holder<ConfiguredPower<?, ?>> power : holderSet) {
				if (!power.isBound()) continue;
				power.unwrap().map(Optional::of, powers::getResourceKey).ifPresent(powerKey -> {
					if (!container.hasPower(powerKey, powerSource))
						container.addPower(powerKey, powerSource);
				});
			}
		}
	}

	@Override
	public @NotNull ResourceKey<Origin> getOrigin(@NotNull ResourceKey<OriginLayer> layer) {
		return this.layers.getOrDefault(layer, OriginRegisters.EMPTY.getKey());
	}

	@Override
	public boolean hasOrigin(@NotNull ResourceKey<OriginLayer> layer) {
		return !Objects.equals(this.getOrigin(layer), OriginRegisters.EMPTY.getKey());
	}

	@Override
	public boolean hadAllOrigins() {
		return this.hadAllOrigins.get();
	}

	@Override
	@NotNull
	public Map<ResourceKey<OriginLayer>, ResourceKey<Origin>> getOrigins() {
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
			IPowerContainer.get(this.player).ifPresent(this::applyCleanup);
		}
		if (this.shouldSync() && !this.player.level.isClientSide() && this.syncCooldown.decrementAndGet() <= 0) {
			OriginsCommon.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this.player), this.getSynchronizationPacket());
			this.syncCooldown.set(20);
			ApoliAPI.synchronizePowerContainer(this.player);
		}
	}

	private void applyCleanup(@NotNull IPowerContainer container) {
		Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry();
		Registry<OriginLayer> layersRegistry = OriginsAPI.getLayersRegistry();
		Iterator<Map.Entry<ResourceKey<OriginLayer>, ResourceKey<Origin>>> iterator = this.layers.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<ResourceKey<OriginLayer>, ResourceKey<Origin>> entry = iterator.next();
			ResourceKey<OriginLayer> layer = entry.getKey();
			ResourceKey<Origin> origin = entry.getValue();
			ResourceLocation powerSource = OriginsAPI.getPowerSource(origin);
			if (!layersRegistry.containsKey(layer)) {
				iterator.remove();
				container.removeAllPowersFromSource(powerSource);
				Origins.LOGGER.debug("CLEANUP: Removed missing layer {} on player {}", layer, this.player.getScoreboardName());
				continue;
			}
			if (!originsRegistry.containsKey(origin)) {
				Origins.LOGGER.debug("CLEANUP: Removed missing origin {} on player {}", origin, this.player.getScoreboardName());
				container.removeAllPowersFromSource(powerSource);
				entry.setValue(OriginRegisters.EMPTY.getKey());
				continue;
			}
			Set<ResourceKey<ConfiguredPower<?, ?>>> currentPowers = ImmutableSet.copyOf(container.getPowersFromSource(powerSource));
			Registry<ConfiguredPower<?, ?>> registry = ApoliAPI.getPowers(this.player.getServer());

			Set<ResourceKey<ConfiguredPower<?, ?>>> newPowers = originsRegistry.getOrThrow(origin).getValidPowers().flatMap(holder -> {
				if (!holder.isBound())
					return Stream.empty();
				Optional<ResourceKey<ConfiguredPower<?, ?>>> key = holder.unwrap().map(Optional::of, registry::getResourceKey);
				if (key.isEmpty())
					return Stream.empty();
				HashSet<ResourceKey<ConfiguredPower<?, ?>>> names = new HashSet<>();
				names.add(key.get());
				names.addAll(holder.value().getChildrenKeys());
				return names.stream();
			}).collect(ImmutableSet.toImmutableSet());
			Set<ResourceKey<ConfiguredPower<?, ?>>> toRemove = currentPowers.stream().filter(x -> !newPowers.contains(x)).collect(Collectors.toSet());
			Set<ResourceKey<ConfiguredPower<?, ?>>> toAdd = newPowers.stream().filter(x -> !currentPowers.contains(x)).collect(Collectors.toSet());
			if (!toRemove.isEmpty()) {
				toRemove.forEach(power -> container.removePower(power, powerSource));
				Origins.LOGGER.debug("CLEANUP: Revoked {} removed powers for origin {} on player {}", toRemove.size(), origin, this.player.getScoreboardName());
			}
			if (!toAdd.isEmpty()) {
				toAdd.forEach(power -> container.addPower(power, powerSource));
				Origins.LOGGER.debug("CLEANUP: Granted {} missing powers for origin {} on player {}", toAdd.size(), origin, this.player.getScoreboardName());
			}
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
		this.layers.forEach((layer, origin) -> builder.put(layer.location(), origin.location()));
		return builder.build();
	}

	@Override
	public boolean checkAutoChoosingLayers(boolean includeDefaults) {
		boolean choseOneAutomatically = false;
		for (Holder.Reference<OriginLayer> layer : OriginsAPI.getActiveLayers()) {
			boolean shouldContinue = false;
			if (!this.hasOrigin(layer.key())) {
				if (includeDefaults && layer.value().hasDefaultOrigin() && !layer.value().defaultOrigin().is(OriginRegisters.EMPTY.getId())) {
					this.setOrigin(layer, layer.value().defaultOrigin());
					choseOneAutomatically = true;
					shouldContinue = true;
				} else {
					Optional<Holder<Origin>> automaticOrigin = layer.value().getAutomaticOrigin(this.player);
					if (automaticOrigin.isPresent()) {
						this.setOrigin(layer, automaticOrigin.get());
						choseOneAutomatically = true;
						shouldContinue = true;
					} else if (layer.value().getOriginOptionCount(this.player) == 0)
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
    @SuppressWarnings({"rawtypes", "unchecked"})
	public void onChosen(@NotNull ResourceKey<Origin> origin, boolean isOrb) {
        Set<ResourceKey<ConfiguredPower<?, ?>>> set = Sets.newHashSet();
        IPowerContainer.get(this.player).ifPresent(container -> container.getPowersFromSource(OriginsAPI.getPowerSource(origin)).stream()
                .map(container::getPower)
                .filter(Objects::nonNull)
                .forEach(power -> {
                    if (power.isBound() && power.value().getFactory() instanceof IOriginCallbackPower callbackPower) {
                        if (!callbackPower.isReady(power.value(), this.player, isOrb)) {
                            callbackPower.prepare(power.value(), this.player, isOrb);
                            power.unwrapKey().ifPresent(k -> set.add((ResourceKey<ConfiguredPower<?, ?>>) (Object) k));
                        } else
                            callbackPower.onChosen(power.value(), this.player, isOrb);
                    }
                }));
        if (!set.isEmpty() && !this.player.level.isClientSide()) {
            OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), new S2COpenWaitingForPowersScreen(isOrb, set));
        }
    }

	@Override
    @SuppressWarnings({"rawtypes", "unchecked"})
	public void onChosen(boolean isOrb) {
        Set<ResourceKey<ConfiguredPower<?, ?>>> set = Sets.newHashSet();
        IPowerContainer.get(this.player).ifPresent(container -> container.getPowers().forEach(x -> {
            if (x.isBound() && x.value().getFactory() instanceof IOriginCallbackPower callbackPower) {
                if (!callbackPower.isReady(x.value(), this.player, isOrb)) {
                    callbackPower.prepare(x.value(), this.player, isOrb);
                    x.unwrapKey().ifPresent(set::add);
                } else
                    callbackPower.onChosen(x.value(), this.player, isOrb);
            }
        }));
        if (!set.isEmpty() && !this.player.level.isClientSide()) {
            OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) this.player), new S2COpenWaitingForPowersScreen(isOrb, set));
        }
    }

	@Override
	public void onReload(@NotNull ICalioDynamicRegistryManager registry) {
		this.cleanupPowers = true;
	}

	@Deprecated
	private final Lazy<OriginComponent> component = Lazy.of(() -> new PlayerOriginComponent(this));

	@Override
	@Deprecated
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
		Registry<OriginLayer> layers = OriginsAPI.getLayersRegistry(this.player.getServer());
		Registry<Origin> origins = OriginsAPI.getOriginsRegistry(this.player.getServer());
		for (Map.Entry<ResourceLocation, ResourceLocation> entry : map.entrySet()) {
			ResourceKey<OriginLayer> layer = ResourceKey.create(OriginsDynamicRegistries.LAYERS_REGISTRY, entry.getKey());
			ResourceKey<Origin> origin = ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, entry.getValue());
			if (layers.containsKey(layer) && origins.containsKey(origin))
				this.layers.put(layer, origin);
		}
		this.hadAllOrigins.set(hadAllOrigins);
	}

	@Override
	public Tag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		CompoundTag layers = new CompoundTag();
		Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry(this.player.getServer());
		Registry<OriginLayer> layersRegistry = OriginsAPI.getLayersRegistry(this.player.getServer());
		for (Map.Entry<ResourceKey<OriginLayer>, ResourceKey<Origin>> entry : this.getOrigins().entrySet()) {
			if (!layersRegistry.containsKey(entry.getKey()) || !originsRegistry.containsKey(entry.getValue())) {
				Origins.LOGGER.warn("Removed missing entry {}: {}", entry.getKey(), entry.getValue());
				continue;
			}
			layers.putString(entry.getKey().location().toString(), entry.getValue().location().toString());
		}
		tag.put("Origins", layers);
		tag.putBoolean("HadAllOrigins", this.hasAllOrigins());
		return tag;
	}

	@Override
	public void deserializeNBT(Tag nbt) {
		this.layers.clear();
		CompoundTag tag = (CompoundTag) nbt;
		CompoundTag layers = tag.getCompound("Origins");
		Registry<OriginLayer> layersRegistry = OriginsAPI.getLayersRegistry(this.player.getServer());
		Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry(this.player.getServer());
		for (String key : layers.getAllKeys()) {
			String origin = layers.getString(key);
			if (origin.isBlank())
				continue;
			ResourceLocation orig = ResourceLocation.tryParse(origin);
			if (orig == null) {
				Origins.LOGGER.warn("Invalid origin {} found for layer {} on entity {}", origin, key, this.player.getScoreboardName());
				continue;
			}
            ResourceKey<Origin> originKey = ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, orig);
			Optional<Holder<Origin>> origin1 = originsRegistry.getHolder(originKey);
			if (origin1.isEmpty() || !origin1.get().isBound()) {
				Origins.LOGGER.warn("Missing origin {} found for layer {} on entity {}", origin, key, this.player.getScoreboardName());
				IPowerContainer.get(this.player).ifPresent(container -> container.removeAllPowersFromSource(OriginsAPI.getPowerSource(orig)));
				continue;
			}
			ResourceLocation rl = ResourceLocation.tryParse(key);
			if (rl == null) {
				Origins.LOGGER.warn("Invalid layer found {} on entity {}", key, this.player.getScoreboardName());
				IPowerContainer.get(this.player).ifPresent(container -> container.removeAllPowersFromSource(OriginsAPI.getPowerSource(origin1.get())));
				continue;
			}
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(OriginsDynamicRegistries.LAYERS_REGISTRY, rl);
			Optional<Holder<OriginLayer>> layer = layersRegistry.getHolder(layerKey);
			if (layer.isEmpty() || !layer.get().isBound()) {
				Origins.LOGGER.warn("Missing layer {} on entity {}", rl, this.player.getScoreboardName());
				IPowerContainer.get(this.player).ifPresent(container -> container.removeAllPowersFromSource(OriginsAPI.getPowerSource(origin1.get())));
				continue;
			}
			this.setOriginInternal(layerKey, originKey, false);
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
