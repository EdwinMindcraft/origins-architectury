package io.github.edwinmindcraft.origins.common.network;

import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2COpenOriginScreen(boolean showDirtBackground) implements CustomPacketPayload {
	public static final ResourceLocation ID = ApoliAPI.identifier("open_origin_screen");
	public static final Type<S2COpenOriginScreen> TYPE = new Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenOriginScreen> STREAM_CODEC = StreamCodec.of(S2COpenOriginScreen::encode, S2COpenOriginScreen::decode);

	public static S2COpenOriginScreen decode(FriendlyByteBuf buf) {
		return new S2COpenOriginScreen(buf.readBoolean());
	}

	public static void encode(FriendlyByteBuf buf, S2COpenOriginScreen packet) {
		buf.writeBoolean(packet.showDirtBackground());
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			OriginsClient.AWAITING_DISPLAY.set(true);
			OriginsClient.SHOW_DIRT_BACKGROUND = this.showDirtBackground();
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
