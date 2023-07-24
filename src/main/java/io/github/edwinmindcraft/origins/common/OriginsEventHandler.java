package io.github.edwinmindcraft.origins.common;

import com.google.common.collect.ImmutableSet;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.mixin.EntityAccessor;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.command.OriginCommand;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.registry.ModDamageSources;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.calio.api.event.DynamicRegistrationEvent;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsBuiltinRegistries;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.capabilities.OriginContainer;
import io.github.edwinmindcraft.origins.common.data.LayerLoader;
import io.github.edwinmindcraft.origins.common.data.OriginLoader;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Origins.MODID)
public class OriginsEventHandler {
	//region Reflection
	private static final Method DECREASE_AIR_SUPPLY = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_7302_", int.class);
	private static final Method INCREASE_AIR_SUPPLY = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_7305_", int.class);

	private static int increaseAirSupply(LivingEntity living, int value) {
		try {
			return (int) INCREASE_AIR_SUPPLY.invoke(living, value);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static int decreaseAirSupply(LivingEntity living, int value) {
		try {
			return (int) DECREASE_AIR_SUPPLY.invoke(living, value);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	//endregion

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		OriginCommand.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player)
			event.addCapability(OriginContainer.ID, new OriginContainer(player));
	}

	@SubscribeEvent
	public static void onDataSync(OnDatapackSyncEvent event) {
		PacketDistributor.PacketTarget target = event.getPlayer() == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(event::getPlayer);
		OriginsCommon.CHANNEL.send(target, BadgeManager.createPacket());
		if (event.getPlayer() != null)
			IOriginContainer.get(event.getPlayer()).map(IOriginContainer::getSynchronizationPacket).ifPresent(packet -> OriginsCommon.CHANNEL.send(target, packet));
	}

	@SubscribeEvent
	public static void addRegistries(CalioDynamicRegistryEvent.Initialize event) {
		ICalioDynamicRegistryManager registryManager = event.getRegistryManager();
		registryManager.addForge(OriginsDynamicRegistries.ORIGINS_REGISTRY, OriginsBuiltinRegistries.ORIGINS, Origin.CODEC);
		registryManager.addReload(OriginsDynamicRegistries.ORIGINS_REGISTRY, "origins", OriginLoader.INSTANCE);
		registryManager.addValidation(OriginsDynamicRegistries.ORIGINS_REGISTRY, OriginLoader.INSTANCE, Origin.class, ApoliDynamicRegistries.CONFIGURED_POWER_KEY);

		registryManager.add(OriginsDynamicRegistries.LAYERS_REGISTRY, OriginLayer.CODEC);
		registryManager.addReload(OriginsDynamicRegistries.LAYERS_REGISTRY, "origin_layers", LayerLoader.INSTANCE);
		registryManager.addValidation(OriginsDynamicRegistries.LAYERS_REGISTRY, LayerLoader.INSTANCE, OriginLayer.class, OriginsDynamicRegistries.ORIGINS_REGISTRY);
	}

	@SubscribeEvent
	public static void onAdvancement(AdvancementEvent event) {
		Advancement advancement = event.getAdvancement();
		IOriginContainer.get(event.getPlayer()).ifPresent(container -> container.getOrigins().forEach((layer, origin) -> origin.getUpgrades().stream().filter(x -> Objects.equals(x.advancement(), advancement.getId())).findFirst().ifPresent(upgrade -> {
			try {
				Origin target = OriginsAPI.getOriginsRegistry().get(upgrade.origin());
				if (target != null) {
					container.setOrigin(layer, target);
					container.synchronize();
					if (!upgrade.announcement().isBlank())
						event.getPlayer().displayClientMessage(new TranslatableComponent(upgrade.announcement()).withStyle(ChatFormatting.GOLD), false);
				}
			} catch (IllegalArgumentException e) {
				Origins.LOGGER.error("Could not perform Origins upgrade from {} to {}, as the upgrade origin did not exist!", origin.getRegistryName(), upgrade.origin());
			}
		})));
	}

	@SubscribeEvent
	@SuppressWarnings("deprecation")
	public static void reloadComplete(CalioDynamicRegistryEvent.LoadComplete event) {
		OriginRegistry.clear();
		OriginLayers.clear();
		MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
		if (currentServer != null) {
			for (ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
				//Revoke any power that would have been removed from the origin.
				IOriginContainer.get(player).ifPresent(IOriginContainer::onReload);
			}
		}
		//Update specs with currently loaded origins.
		if (OriginsConfigs.COMMON.updateOriginList(event.getRegistryManager().get(OriginsDynamicRegistries.ORIGINS_REGISTRY))
			&& OriginsConfigs.COMMON_SPECS.isLoaded())
			OriginsConfigs.COMMON_SPECS.save();
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getPlayer() instanceof ServerPlayer sp && !event.getPlayer().level.isClientSide())
			Objects.requireNonNull(sp.getServer()).submitAsync(() -> IOriginContainer.get(sp).ifPresent(container -> {
				if (!container.hasAllOrigins()) {
					container.checkAutoChoosingLayers(true);
					OriginsCommon.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp), container.getSynchronizationPacket());
					container.synchronize();
					if (container.hasAllOrigins())
						container.onChosen(false);
					else
						OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new S2COpenOriginScreen(true));
				}
			}));
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Player target && event.getPlayer() instanceof ServerPlayer sp && !event.getPlayer().level.isClientSide())
			Objects.requireNonNull(sp.getServer()).submitAsync(() -> IOriginContainer.get(target).ifPresent(x -> OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), x.getSynchronizationPacket())));
	}

	@SubscribeEvent
	public static void playerClone(PlayerEvent.Clone event) {
		event.getOriginal().reviveCaps(); // Reload capabilities.

		LazyOptional<IOriginContainer> original = IOriginContainer.get(event.getOriginal());
		LazyOptional<IOriginContainer> player = IOriginContainer.get(event.getPlayer());
		if (original.isPresent() != player.isPresent()) {
			Apoli.LOGGER.info("Capability mismatch: original:{}, new:{}", original.isPresent(), player.isPresent());
		}
		player.ifPresent(p -> original.ifPresent(o -> p.deserializeNBT(o.serializeNBT())));

		event.getOriginal().invalidateCaps(); // Unload capabilities.
	}

    @SubscribeEvent
    public static void playerChangedDimensions(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer)
            IOriginContainer.get(event.getEntity()).ifPresent(IOriginContainer::synchronize);
    }

	@SubscribeEvent
	public static void onPlayerTickEnd(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			Player player = event.player;
			IOriginContainer.get(event.player).ifPresent(IOriginContainer::tick);
			if (IPowerContainer.hasPower(player, OriginsPowerTypes.WATER_BREATHING.get())) {
				if (!player.isEyeInFluid(FluidTags.WATER) && !player.hasEffect(MobEffects.WATER_BREATHING) && !player.hasEffect(MobEffects.CONDUIT_POWER)) {
					if (!((EntityAccessor) player).callIsBeingRainedOn()) {
						int landGain = increaseAirSupply(player, 0);
						player.setAirSupply(decreaseAirSupply(player, player.getAirSupply()) - landGain);
						if (player.getAirSupply() == -20) {
							player.setAirSupply(0);

							for (int i = 0; i < 8; ++i) {
								double f = player.getRandom().nextDouble() - player.getRandom().nextDouble();
								double g = player.getRandom().nextDouble() - player.getRandom().nextDouble();
								double h = player.getRandom().nextDouble() - player.getRandom().nextDouble();
								player.level.addParticle(ParticleTypes.BUBBLE, player.getRandomX(0.5), player.getEyeY() + player.getRandom().nextGaussian() * 0.08D, player.getRandomZ(0.5), f * 0.5F, g * 0.5F + 0.25F, h * 0.5F);
							}
							player.hurt(ModDamageSources.NO_WATER_FOR_GILLS, 2.0F);
						}
					} else {
						int landGain = increaseAirSupply(player, 0);
						player.setAirSupply(player.getAirSupply() - landGain);
					}
				} else if (player.getAirSupply() < player.getMaxAirSupply()) {
					player.setAirSupply(increaseAirSupply(player, player.getAirSupply()));
				}
			}
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
			HashSet<ResourceLocation> powers = new HashSet<>(original.getPowers());
			powers.removeIf(x -> !OriginsConfigs.COMMON.isPowerEnabled(event.getRegistryName(), x));
			if (powers.size() != original.getPowers().size()) {
				Origins.LOGGER.info("Powers [{}] were disabled by config for origin: {}", original.getPowers().stream()
								.filter(x -> !powers.contains(x))
								.map(ResourceLocation::toString)
								.collect(Collectors.joining(",")),
						event.getRegistryName());
				event.setNewEntry(new Origin(ImmutableSet.copyOf(powers), original.getIcon(), original.isUnchoosable(),
						original.getOrder(), original.getImpact(), original.getName(), original.getDescription(),
						original.getUpgrades(), original.isSpecial()));
			}
		}
	}
}
