package io.github.edwinmindcraft.origins.common.network;

import io.github.apace100.apoli.Apoli;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.function.Supplier;

public record C2SChooseOrigin(ResourceLocation layer, ResourceLocation origin) {

	public static C2SChooseOrigin decode(FriendlyByteBuf buf) {
		return new C2SChooseOrigin(buf.readResourceLocation(), buf.readResourceLocation());
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceLocation(this.layer());
		buf.writeResourceLocation(this.origin());
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			ServerPlayer sender = contextSupplier.get().getSender();
			if (sender == null) return;
			IOriginContainer.get(sender).ifPresent(container -> {
				Optional<Holder<OriginLayer>> layer = OriginsAPI.getLayersRegistry().getHolder(ResourceKey.create(OriginsDynamicRegistries.LAYERS_REGISTRY, this.layer())).filter(Holder::isBound);
				if (layer.isEmpty()) {
					Origins.LOGGER.warn("Player {} tried to select an origin for missing layer {}", sender.getScoreboardName(), this.layer());
					return;
				}
				if (container.hasAllOrigins() || container.hasOrigin(layer.get())) {
					Origins.LOGGER.warn("Player {} tried to choose origin for layer {} while having one already.", sender.getScoreboardName(), this.layer());
					return;
				}
				Optional<Holder<Origin>> origin = OriginsAPI.getOriginsRegistry().getHolder(ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, this.origin())).filter(Holder::isBound);
				if (origin.isEmpty()) {
					Origins.LOGGER.warn("Player {} chose unknown origin: {} for layer {}", sender.getScoreboardName(), this.origin(), this.layer());
					return;
				}
				if (!origin.get().value().isChoosable() || !layer.get().value().contains(this.origin(), sender)) {
					Origins.LOGGER.warn("Player {} tried to choose invalid origin: {} for layer: {}", sender.getScoreboardName(), this.origin(), this.layer());
					container.setOrigin(layer.get(), OriginRegisters.EMPTY.getHolder().orElseThrow());
				} else {
                    try {
                        boolean hadOriginBefore = container.hadAllOrigins();
                        boolean hadAllOrigins = container.hasAllOrigins();
                        container.setOrigin(layer.get(), origin.get());
                        container.checkAutoChoosingLayers(false);
                        if (container.hasAllOrigins() && !hadAllOrigins)
                            container.onChosen(hadOriginBefore);
                    } catch (Exception e) {
                        Apoli.LOGGER.error(e.toString());
                        for (StackTraceElement el : e.getStackTrace()) {
                            Apoli.LOGGER.error("\tat "+ el);
                        }
                    }
				}
				container.synchronize();
				OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new S2CConfirmOrigin(this.layer(), this.origin()));
			});
		});
		contextSupplier.get().setPacketHandled(true);
	}
}
