package io.github.edwinmindcraft.origins.common;

import io.github.apace100.calio.Calio;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.BadgeFactories;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.calio.api.registry.ICalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsBuiltinRegistries;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.data.LayerLoader;
import io.github.edwinmindcraft.origins.common.data.OriginLoader;
import io.github.edwinmindcraft.origins.common.network.*;
import io.github.edwinmindcraft.origins.common.registry.OriginArgumentTypes;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class OriginsCommon {
	private static final String NETWORK_VERSION = "1.2";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Origins.identifier("network"), () -> NETWORK_VERSION, NETWORK_VERSION::equals, NETWORK_VERSION::equals);

	private static <T> Function<FriendlyByteBuf, T> withLogging(Function<FriendlyByteBuf, T> original) {
		return buf -> {
			T apply = original.apply(buf);
			if (Calio.isDebugMode())
				Origins.LOGGER.info("Received packet: {}", apply);
			return apply;
		};
	}

	private static void initializeNetwork() {
		int message = 0;
		CHANNEL.messageBuilder(S2CSynchronizeOrigin.class, message++, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(S2CSynchronizeOrigin::encode).decoder(withLogging(S2CSynchronizeOrigin::decode))
				.consumerNetworkThread(S2CSynchronizeOrigin::handle).add();
		CHANNEL.messageBuilder(S2COpenOriginScreen.class, message++, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(S2COpenOriginScreen::encode).decoder(withLogging(S2COpenOriginScreen::decode))
				.consumerNetworkThread(S2COpenOriginScreen::handle).add();
		CHANNEL.messageBuilder(S2CConfirmOrigin.class, message++, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(S2CConfirmOrigin::encode).decoder(withLogging(S2CConfirmOrigin::decode))
				.consumerNetworkThread(S2CConfirmOrigin::handle).add();
		CHANNEL.messageBuilder(S2CSynchronizeBadges.class, message++, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(S2CSynchronizeBadges::encode).decoder(withLogging(S2CSynchronizeBadges::decode))
				.consumerNetworkThread(S2CSynchronizeBadges::handle).add();
        CHANNEL.messageBuilder(S2COpenWaitingForPowersScreen.class, message++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenWaitingForPowersScreen::encode).decoder(withLogging(S2COpenWaitingForPowersScreen::decode))
                .consumerNetworkThread(S2COpenWaitingForPowersScreen::handle).add();

		CHANNEL.messageBuilder(C2SChooseRandomOrigin.class, message++, NetworkDirection.PLAY_TO_SERVER)
				.encoder(C2SChooseRandomOrigin::encode).decoder(withLogging(C2SChooseRandomOrigin::decode))
				.consumerNetworkThread(C2SChooseRandomOrigin::handle).add();
		CHANNEL.messageBuilder(C2SChooseOrigin.class, message++, NetworkDirection.PLAY_TO_SERVER)
				.encoder(C2SChooseOrigin::encode).decoder(withLogging(C2SChooseOrigin::decode))
				.consumerNetworkThread(C2SChooseOrigin::handle).add();
		CHANNEL.messageBuilder(C2SAcknowledgeOrigins.class, message++, NetworkDirection.PLAY_TO_SERVER)
				.encoder(C2SAcknowledgeOrigins::encode).decoder(withLogging(C2SAcknowledgeOrigins::decode))
				.consumerNetworkThread(C2SAcknowledgeOrigins::handle).add();
        CHANNEL.messageBuilder(C2SFinalizeNowReadyPowers.class, message++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SFinalizeNowReadyPowers::encode).decoder(withLogging(C2SFinalizeNowReadyPowers::decode))
                .consumerNetworkThread(C2SFinalizeNowReadyPowers::handle).add();

		Origins.LOGGER.debug("Registered {} packets", message);
	}

	public static void initialize() {
		IEventBus mod = FMLJavaModLoadingContext.get().getModEventBus();
		OriginRegisters.register();
		OriginsPowerTypes.register();
		BadgeFactories.bootstrap();
		BadgeManager.init();
		mod.addListener(OriginsCommon::initializeDynamicRegistries);
		mod.addListener(OriginsCommon::registerCapabilities);
		mod.addListener(OriginsCommon::commonSetup);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.register(IOriginContainer.class);
	}

	public static void commonSetup(FMLCommonSetupEvent event) {
		initializeNetwork();
		event.enqueueWork(OriginArgumentTypes::initialize);
	}

	public static void initializeDynamicRegistries(CalioDynamicRegistryEvent.Initialize event) {
		ICalioDynamicRegistryManager registryManager = event.getRegistryManager();
		registryManager.addForge(OriginsDynamicRegistries.ORIGINS_REGISTRY, OriginsBuiltinRegistries.ORIGINS, Origin.CODEC);
		registryManager.addReload(OriginsDynamicRegistries.ORIGINS_REGISTRY, "origins", OriginLoader.INSTANCE);
		registryManager.addValidation(OriginsDynamicRegistries.ORIGINS_REGISTRY, OriginLoader.INSTANCE, Origin.class, ApoliDynamicRegistries.CONFIGURED_POWER_KEY);

		registryManager.add(OriginsDynamicRegistries.LAYERS_REGISTRY, OriginLayer.CODEC);
		registryManager.addReload(OriginsDynamicRegistries.LAYERS_REGISTRY, "origin_layers", LayerLoader.INSTANCE);
		registryManager.addValidation(OriginsDynamicRegistries.LAYERS_REGISTRY, LayerLoader.INSTANCE, OriginLayer.class, OriginsDynamicRegistries.ORIGINS_REGISTRY);
	}
}
