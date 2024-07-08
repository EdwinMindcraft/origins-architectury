package io.github.apace100.origins;

import com.google.common.collect.ImmutableList;
import io.github.apace100.apoli.util.NamespaceAlias;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.power.OriginsEntityConditions;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.registry.*;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.OriginsConfigs;
import io.github.edwinmindcraft.origins.common.registry.OriginArgumentTypes;
import io.github.edwinmindcraft.origins.data.OriginsData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Origins.MODID)
public class Origins {

	public static final String MODID = OriginsAPI.MODID;
	public static String VERSION = "";
	public static final Logger LOGGER = LogManager.getLogger(Origins.class);

	@Deprecated
	public static ServerConfig config = new ServerConfig();

	public Origins(IEventBus bus) {
		VERSION = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion().toString();
		LOGGER.info("Origins " + VERSION + " is initializing. Have fun!");
		ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, OriginsConfigs.COMMON_SPECS);
		ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, OriginsConfigs.CLIENT_SPECS);
		ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, OriginsConfigs.SERVER_SPECS);

		NamespaceAlias.addAlias(MODID, "apoli");

		OriginsPowerTypes.register();
		OriginsEntityConditions.register();
		OriginArgumentTypes.bootstrap();

		ModBlocks.register();
		ModItems.register();
		ModTags.register();
		ModEnchantments.register();
		ModEntities.register();
		ModLoot.register();

		OriginsCommon.initialize(bus);
		OriginsData.initialize();

		NamespaceAlias.addAlias("origins", "apoli");

		// FIXME: Register criterions like a regular registry.
		// CriteriaTriggers.register(ChoseOriginCriterion.INSTANCE);
	}

	public static ResourceLocation identifier(String path) {
		return ResourceLocation.fromNamespaceAndPath(Origins.MODID, path);
	}

	@Deprecated
	public static class ServerConfig {
		@Deprecated
		public boolean isOriginDisabled(ResourceLocation originId) {
			return !OriginsConfigs.COMMON.isOriginEnabled(originId);
		}

		@Deprecated
		public boolean isPowerDisabled(ResourceLocation originId, ResourceLocation powerId) {
			return !OriginsConfigs.COMMON.isPowerEnabled(originId, powerId);
		}

		@Deprecated
		public boolean addToConfig(Origin origin) {
			return OriginsConfigs.COMMON.updateOriginList(CalioAPI.getSidedRegistryAccess(), ImmutableList.of(origin.getWrapped()));
		}
	}
}
