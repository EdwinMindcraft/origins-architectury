package io.github.edwinmindcraft.origins.common;

import io.github.apace100.origins.badge.BadgeFactories;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.registry.ModItems;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.calio.api.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.data.LayerLoader;
import io.github.edwinmindcraft.origins.common.data.OriginLoader;
import io.github.edwinmindcraft.origins.common.network.*;
import io.github.edwinmindcraft.origins.common.registry.OriginArgumentTypes;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class OriginsCommon {
	private static final String NETWORK_VERSION = "2.0";

	private static void initializeNetwork(RegisterPayloadHandlersEvent event) {
		event.registrar(CalioAPI.MODID)
				.versioned(NETWORK_VERSION)
				.playToClient(S2CSynchronizeOrigin.TYPE, S2CSynchronizeOrigin.STREAM_CODEC, S2CSynchronizeOrigin::handle)
				.playToClient(S2COpenOriginScreen.TYPE, S2COpenOriginScreen.STREAM_CODEC, S2COpenOriginScreen::handle)
				.playToClient(S2CConfirmOrigin.TYPE, S2CConfirmOrigin.STREAM_CODEC, S2CConfirmOrigin::handle)
				.playToClient(S2CSynchronizeBadges.TYPE, S2CSynchronizeBadges.STREAM_CODEC, S2CSynchronizeBadges::handle)
				.playToClient(S2COpenWaitingForPowersScreen.TYPE, S2COpenWaitingForPowersScreen.STREAM_CODEC, S2COpenWaitingForPowersScreen::handle)
				.playToServer(C2SChooseRandomOrigin.TYPE, C2SChooseRandomOrigin.STREAM_CODEC, C2SChooseRandomOrigin::handle)
				.playToServer(C2SChooseOrigin.TYPE, C2SChooseOrigin.STREAM_CODEC, C2SChooseOrigin::handle)
				.playToServer(C2SAcknowledgeOrigins.TYPE, C2SAcknowledgeOrigins.STREAM_CODEC, C2SAcknowledgeOrigins::handle)
				.playToServer(C2SFinalizeNowReadyPowers.TYPE, C2SFinalizeNowReadyPowers.STREAM_CODEC, C2SFinalizeNowReadyPowers::handle);
	}

	public static void initialize(IEventBus bus) {
		OriginRegisters.register(bus);
		OriginsPowerTypes.register();
		BadgeFactories.bootstrap();
		BadgeManager.init();
		OriginArgumentTypes.initialize();
		bus.addListener(OriginsCommon::initializeDynamicRegistries);
		bus.addListener(OriginsCommon::initializeNetwork);
		bus.addListener(OriginsCommon::modifyCreativeTabs);
	}

	public static void initializeDynamicRegistries(CalioDynamicRegistryEvent.Initialize event) {
		CalioDynamicRegistryManager registryManager = event.getRegistryManager();
		registryManager.addReload(OriginsDynamicRegistries.ORIGINS_REGISTRY, "origins", OriginLoader.INSTANCE);
		registryManager.addValidation(OriginsDynamicRegistries.ORIGINS_REGISTRY, OriginLoader.INSTANCE, Origin.class, ApoliDynamicRegistries.CONFIGURED_POWER_KEY);

		registryManager.add(OriginsDynamicRegistries.LAYERS_REGISTRY, OriginLayer.CODEC);
		registryManager.addReload(OriginsDynamicRegistries.LAYERS_REGISTRY, "origin_layers", LayerLoader.INSTANCE);
		registryManager.addValidation(OriginsDynamicRegistries.LAYERS_REGISTRY, LayerLoader.INSTANCE, OriginLayer.class, OriginsDynamicRegistries.ORIGINS_REGISTRY);
	}

	public static void modifyCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			event.accept(ModItems.ORB_OF_ORIGIN.get());
		}
	}
}
