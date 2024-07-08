package io.github.edwinmindcraft.origins.common.network;

import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import io.github.edwinmindcraft.origins.client.OriginsClientUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public record S2CConfirmOrigin(ResourceLocation layer, ResourceLocation origin) implements CustomPacketPayload {
	public static final ResourceLocation ID = ApoliAPI.identifier("confirm_origin");
	public static final Type<S2CConfirmOrigin> TYPE = new Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, S2CConfirmOrigin> STREAM_CODEC = StreamCodec.of(S2CConfirmOrigin::encode, S2CConfirmOrigin::decode);

	public static S2CConfirmOrigin decode(FriendlyByteBuf buf) {
		return new S2CConfirmOrigin(buf.readResourceLocation(), buf.readResourceLocation());
	}

	public static void encode(FriendlyByteBuf buf, S2CConfirmOrigin packet) {
		buf.writeResourceLocation(packet.layer());
		buf.writeResourceLocation(packet.origin());
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = OriginsClientUtils.getClientPlayer();
			if (player == null) return;
			OriginLayer layer = OriginsAPI.getLayersRegistry().get(this.layer());
			Origin origin = OriginsAPI.getOriginsRegistry().get(this.origin());
			if (layer == null || origin == null) {
				Origins.LOGGER.warn("Received invalid confirmation: {} ({}): {} ({})", this.layer(), layer, this.origin(), origin);
				return;
			}
			// FIXME: OriginContainer.
			IOriginContainer.get(player).ifPresent(x -> x.setOrigin(layer, origin));
			OriginsClient.OPEN_NEXT_LAYER.set(true);
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return null;
	}
}
