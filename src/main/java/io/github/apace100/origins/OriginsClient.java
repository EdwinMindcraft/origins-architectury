package io.github.apace100.origins;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.apace100.apoli.ApoliClient;
import io.github.apace100.origins.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = Origins.MODID, dist = Dist.CLIENT)
public class OriginsClient {

	public static KeyMapping usePrimaryActivePowerKeybind;
	public static KeyMapping useSecondaryActivePowerKeybind;
	public static KeyMapping viewCurrentOriginKeybind;

	public static void initialize(IEventBus bus) {

		usePrimaryActivePowerKeybind = new KeyMapping("key.origins.primary_active", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "category." + Origins.MODID);
		useSecondaryActivePowerKeybind = new KeyMapping("key.origins.secondary_active", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category." + Origins.MODID);
		viewCurrentOriginKeybind = new KeyMapping("key.origins.view_origin", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "category." + Origins.MODID);

		ApoliClient.registerPowerKeybinding("key.origins.primary_active", usePrimaryActivePowerKeybind);
		ApoliClient.registerPowerKeybinding("key.origins.secondary_active", useSecondaryActivePowerKeybind);
		ApoliClient.registerPowerKeybinding("primary", usePrimaryActivePowerKeybind);
		ApoliClient.registerPowerKeybinding("secondary", useSecondaryActivePowerKeybind);

		// "none" is the default key used when none is specified.
		ApoliClient.registerPowerKeybinding("none", usePrimaryActivePowerKeybind);
		bus.addListener(OriginsClient::clientSetup);
		bus.addListener(OriginsClient::entityRenderers);
		bus.addListener(OriginsClient::registerKeyBindings);
	}

	public static void clientSetup(FMLClientSetupEvent event) {}

	private static void registerKeyBindings(RegisterKeyMappingsEvent event) {
		event.register(usePrimaryActivePowerKeybind);
		event.register(useSecondaryActivePowerKeybind);
		event.register(viewCurrentOriginKeybind);
	}

	public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.ENDERIAN_PEARL.get(), ThrownItemRenderer::new);
	}
}
