package io.github.edwinmindcraft.origins.common;

import com.google.common.collect.ImmutableList;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.mixin.EntityAccessor;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.command.OriginCommand;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.common.ApoliEventHandler;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.calio.api.event.DynamicRegistrationEvent;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = Origins.MODID)
public class OriginsEventHandler {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		OriginCommand.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onDataSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() == null) {
			PacketDistributor.sendToAllPlayers(BadgeManager.createPacket());
		} else {
			PacketDistributor.sendToPlayer(event.getPlayer(), BadgeManager.createPacket());
			// FIXME: OriginContainer
			// Optional.ofNullable(IOriginContainer.get(event.getPlayer())).map(IOriginContainer::getSynchronizationPacket).ifPresent(packet -> OriginsCommon.CHANNEL.send(target, packet));
		}
	}

	@SubscribeEvent
	public static void onAdvancement(AdvancementEvent event) {
		AdvancementHolder advancement = event.getAdvancement();
		Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry();
		// FIXME: OriginContainer.
		/*
		IOriginContainer.get(event.getEntity()).ifPresent(container -> container.getOrigins()
				.forEach((layer, origin) -> originsRegistry.getHolder(origin).stream().flatMap(x -> x.get().getUpgrades().stream())
						.filter(x -> Objects.equals(x.advancement(), advancement.getId())).findFirst()
						.ifPresent(upgrade -> {
							try {
								Holder<Origin> target = upgrade.origin();
								if (target.isBound() && target.unwrapKey().isPresent()) {
									container.setOrigin(layer, target.unwrapKey().get());
									container.synchronize();
									if (!upgrade.announcement().isBlank())
										event.getEntity().displayClientMessage(Component.translatable(upgrade.announcement()).withStyle(ChatFormatting.GOLD), false);
								}
							} catch (IllegalArgumentException e) {
								Origins.LOGGER.error("Could not perform Origins upgrade from {} to {}, as the upgrade origin did not exist!", origin.location(), upgrade.origin().unwrapKey().orElse(null));
							}
						})));
		 */
	}

	@SubscribeEvent
	@SuppressWarnings("deprecation")
	public static void reloadComplete(CalioDynamicRegistryEvent.LoadComplete event) {
		OriginRegistry.clear();
		OriginLayers.clear();
		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer != null) {
			for (ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
				IOriginContainer container = IOriginContainer.get(player);
				if (container != null) {
					//Revoke any power that would have been removed from the origin.
					container.onReload(event.getRegistryManager());
				}
			}
		}
		//Update specs with currently loaded origins.
		if (OriginsConfigs.COMMON.updateOriginList(event.getRegistryAccess(), event.getRegistryAccess().registryOrThrow(OriginsDynamicRegistries.ORIGINS_REGISTRY))
			&& OriginsConfigs.COMMON_SPECS.isLoaded())
			OriginsConfigs.COMMON_SPECS.save();
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer sp && !event.getEntity().level().isClientSide())
			Objects.requireNonNull(sp.getServer()).submitAsync(() -> IOriginContainer.get(sp).ifPresent(container -> {
				if (!container.hasAllOrigins()) {
					container.checkAutoChoosingLayers(true);
					OriginsCommon.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp), container.getSynchronizationPacket());
					container.synchronize();
					if (container.hasAllOrigins())
						container.onChosen(false);
					else
						PacketDistributor.sendToPlayer(sp, new S2COpenOriginScreen(true));
				}
			}));
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer sp && !event.getEntity().level().isClientSide())
			Objects.requireNonNull(sp.getServer()).submitAsync(() -> IOriginContainer.get(target).ifPresent(x -> OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), x.getSynchronizationPacket())));
	}

	@SubscribeEvent
	public static void playerClone(PlayerEvent.Clone event) {
		event.getOriginal().reviveCaps(); // Reload capabilities.

		LazyOptional<IOriginContainer> original = IOriginContainer.get(event.getOriginal());
		LazyOptional<IOriginContainer> player = IOriginContainer.get(event.getEntity());
		if (original.isPresent() != player.isPresent()) {
			Apoli.LOGGER.info("Capability mismatch: original:{}, new:{}", original.isPresent(), player.isPresent());
		}
		player.ifPresent(p -> original.ifPresent(o -> p.deserializeNBT(o.serializeNBT())));

		event.getOriginal().invalidateCaps(); // Unload capabilities.
	}

    @SubscribeEvent
    public static void playerChangedDimensions(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer)
            IOriginContainer.get(event.getEntity()).synchronize();
    }

	@SubscribeEvent
	public static void onPlayerTickEnd(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		IOriginContainer.get(event.getEntity()).tick();
	}

	@SubscribeEvent
	public static void modifyBreathing(LivingBreatheEvent event) {
		LivingEntity entity = event.getEntity();
		if (PowerContainer.hasPower(entity, OriginsPowerTypes.WATER_BREATHING.get())) {
            event.setCanBreathe(entity.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value()) || entity.hasEffect(MobEffects.WATER_BREATHING) || entity.hasEffect(MobEffects.CONDUIT_POWER) || ((EntityAccessor) entity).callIsBeingRainedOn());
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onOriginLoad(DynamicRegistrationEvent<Origin> event) {
		if (event.getOriginal().isSpecial()) //Nothing done on special origins
			return;
		if (!OriginsConfigs.COMMON.isOriginEnabled(event.getRegistryName()))
			event.withCancellationReason("Disabled by config").setCanceled(true);
		else {
			Origin original = event.getOriginal();
			Set<Holder<ConfiguredPower<?, ?>>> originalPowers = original.getPowers().stream().flatMap(HolderSet::stream).collect(Collectors.toUnmodifiableSet());
			Set<Holder<ConfiguredPower<?, ?>>> powers = new HashSet<>(originalPowers);
			powers.removeIf(x -> {
				Optional<ResourceKey<ConfiguredPower<?, ?>>> key = x.unwrapKey();
				return key.isEmpty() || !OriginsConfigs.COMMON.isPowerEnabled(event.getRegistryName(), key.get().location());
			});
			if (powers.size() != originalPowers.size()) {
				Origins.LOGGER.info("Powers [{}] were disabled by config for origin: {}", originalPowers.stream()
								.filter(x -> !powers.contains(x))
								.map(x -> x.unwrapKey().orElseThrow().location().toString())
								.collect(Collectors.joining(",")),
						event.getRegistryName());
			}
			powers.removeIf(x -> {
				Optional<ResourceKey<ConfiguredPower<?, ?>>> key = x.unwrapKey();
				return key.isEmpty() || ApoliEventHandler.isPowerDisabled(key.get().location());
			});
			if (powers.size() != originalPowers.size()) {
				event.setNewEntry(new Origin(ImmutableList.of(HolderSet.direct(ImmutableList.copyOf(powers))), original.getIcon(), original.isUnchoosable(),
						original.getOrder(), original.getImpact(), original.getName(), original.getDescription(),
						original.getUpgrades(), original.isSpecial()));
			}
		}
	}

}
