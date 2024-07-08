package io.github.edwinmindcraft.origins.common;

import com.electronwill.nightconfig.core.Config;
import com.google.common.collect.ImmutableList;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class OriginsConfigs {
	public static class Server {
		public Server(ModConfigSpec.Builder builder) {}
	}

	public static class Client {
		public Client(ModConfigSpec.Builder builder) {}
	}

	public static class Common {

		private final ModConfigSpec.ConfigValue<Config> origins;

		public Common(ModConfigSpec.Builder builder) {
			//Remove validation.
			this.origins = builder.define(ImmutableList.of("origins"), Config::inMemory, x -> x instanceof Config, Config.class);
		}

		public boolean isOriginEnabled(ResourceLocation origin) {
			return this.origins.get().getOrElse(ImmutableList.of(origin.toString(), "enabled"), true);
		}

		public boolean isPowerEnabled(ResourceLocation origin, ResourceLocation power) {
			return this.origins.get().getOrElse(ImmutableList.of(origin.toString(), power.toString()), true);
		}

		public boolean updateOriginList(RegistryAccess registries) {
			boolean changed = false;
			Registry<Origin> registry = registries.registryOrThrow(OriginsDynamicRegistries.ORIGINS_REGISTRY);
			Registry<ConfiguredPower<?, ?>> powers = registries.registryOrThrow(ApoliDynamicRegistries.CONFIGURED_POWER_KEY);
			for (Origin origin : registry) {
				ResourceLocation registryName = registry.getKey(origin);
				if (origin.isSpecial() || registryName == null) //Ignore special origins
					continue;
				if (this.origins.get().add(ImmutableList.of(registryName.toString(), "enabled"), true))
					changed = true;
				for (Holder<ConfiguredPower<?, ?>> holder : origin.getValidPowers().toList()) {
					Optional<ResourceKey<ConfiguredPower<?, ?>>> key = holder.unwrap().map(Optional::of, powers::getResourceKey);
					if (key.isPresent() && this.origins.get().add(ImmutableList.of(registryName.toString(), key.get().location().toString()), true)) {
						changed = true;
					}
				}
			}
			return changed;
		}
	}

	public static final ModConfigSpec COMMON_SPECS;
	public static final ModConfigSpec CLIENT_SPECS;
	public static final ModConfigSpec SERVER_SPECS;

	public static final Common COMMON;
	public static final Client CLIENT;
	public static final Server SERVER;

	static {
		Pair<Common, ModConfigSpec> common = new ModConfigSpec.Builder().configure(Common::new);
		Pair<Client, ModConfigSpec> client = new ModConfigSpec.Builder().configure(Client::new);
		Pair<Server, ModConfigSpec> server = new ModConfigSpec.Builder().configure(Server::new);
		COMMON_SPECS = common.getRight();
		CLIENT_SPECS = client.getRight();
		SERVER_SPECS = server.getRight();
		COMMON = common.getLeft();
		CLIENT = client.getLeft();
		SERVER = server.getLeft();
	}
}
