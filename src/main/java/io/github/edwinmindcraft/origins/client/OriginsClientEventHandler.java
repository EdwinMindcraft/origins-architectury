package io.github.edwinmindcraft.origins.client;

import com.google.common.collect.ImmutableList;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.ViewOriginScreen;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import io.github.apace100.origins.util.PowerKeyManager;
import io.github.edwinmindcraft.calio.api.event.CalioDynamicRegistryEvent;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.client.screen.WaitForPowersScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

import static io.github.apace100.origins.OriginsClient.viewCurrentOriginKeybind;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Origins.MODID)
public class OriginsClientEventHandler {

	@SubscribeEvent
	public static void renderTick(TickEvent.RenderTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			Minecraft instance = Minecraft.getInstance();
			if (OriginsClient.AWAITING_DISPLAY.get() && instance.screen == null && instance.player != null) {
				IOriginContainer.get(instance.player).ifPresent(container -> {
					List<Holder.Reference<OriginLayer>> layers = OriginsAPI.getActiveLayers().stream().filter(x -> !container.hasOrigin(x)).sorted(Comparator.comparing(Holder::value)).toList();
					if (layers.size() > 0) {
						instance.setScreen(new ChooseOriginScreen(ImmutableList.copyOf(layers), 0, OriginsClient.SHOW_DIRT_BACKGROUND));
						OriginsClient.AWAITING_DISPLAY.set(false);
					}
				});
			}

			if (OriginsClient.WAITING_FOR_POWERS.get()) {
				Minecraft.getInstance().setScreen(new WaitForPowersScreen(OriginsClient.SHOW_DIRT_BACKGROUND, OriginsClient.WAITING_POWERS, OriginsClient.SELECTION_WAS_ORB));
				OriginsClient.WAITING_POWERS.clear();
				OriginsClient.WAITING_FOR_POWERS.set(false);
			}

			if (OriginsClient.OPEN_NEXT_LAYER.get()) {
				if (instance.screen instanceof WaitForNextLayerScreen screen)
					screen.openSelection();
			}
		}
	}

	@SubscribeEvent
	public static void onKeyPressed(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			while (viewCurrentOriginKeybind.consumeClick()) {
				if (!(Minecraft.getInstance().screen instanceof ViewOriginScreen)) {
					Minecraft.getInstance().setScreen(new ViewOriginScreen());
				}
			}
		}
	}

	@SubscribeEvent
	public static void onCalioRegistryClear(CalioDynamicRegistryEvent.Reload event) {
		PowerKeyManager.clearCache();
	}
}
