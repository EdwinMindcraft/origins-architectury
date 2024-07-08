package io.github.edwinmindcraft.origins.common.network;

import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2SAcknowledgeOrigins implements CustomPacketPayload {
	public static final ResourceLocation ID = Origins.identifier("acknowledge_origins");
	public static final CustomPacketPayload.Type<C2SAcknowledgeOrigins> TYPE = new CustomPacketPayload.Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, C2SAcknowledgeOrigins> STREAM_CODEC = StreamCodec.unit(new C2SAcknowledgeOrigins());

	public C2SAcknowledgeOrigins() {

	}

	public void handle(IPayloadContext context) {
		IOriginContainer.get(context.player()).validateSynchronization();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
