package io.github.edwinmindcraft.origins.common.network;

import com.google.common.collect.ImmutableMap;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.capabilities.OriginContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

public record S2CSynchronizeOrigin(int entity, Map<ResourceLocation, ResourceLocation> origins, boolean hadAllOrigins) implements CustomPacketPayload {
	public static final ResourceLocation ID = ApoliAPI.identifier("sync_origin");
	public static final Type<S2CSynchronizeOrigin> TYPE = new Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, S2CSynchronizeOrigin> STREAM_CODEC = StreamCodec.of(S2CSynchronizeOrigin::encode, S2CSynchronizeOrigin::decode);

	public static void encode(FriendlyByteBuf buf, S2CSynchronizeOrigin packet) {
		buf.writeInt(packet.entity());
		buf.writeVarInt(packet.origins().size());
		this.origins().forEach((layer, origin) -> {
			buf.writeResourceLocation(layer);
			buf.writeResourceLocation(origin);
		});
		buf.writeBoolean(packet.hadAllOrigins());
	}

	public static S2CSynchronizeOrigin decode(FriendlyByteBuf buf) {
		int entity = buf.readInt();
		int size = buf.readVarInt();
		ImmutableMap.Builder<ResourceLocation, ResourceLocation> builder = ImmutableMap.builder();
		for (int i = 0; i < size; i++) {
			builder.put(buf.readResourceLocation(), buf.readResourceLocation());
		}
		boolean hadAll = buf.readBoolean();
		return new S2CSynchronizeOrigin(entity, builder.build(), hadAll);
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			Level level = Minecraft.getInstance().level;
			if (level == null) return;
			Entity entity = level.getEntity(this.entity());
			if (entity == null) return;
			// FIXME: OriginContainer.
			entity.getCapability(OriginsAPI.ORIGIN_CONTAINER).ifPresent(x -> {
				if (x instanceof OriginContainer container) {
					container.acceptSynchronization(this.origins(), this.hadAllOrigins());
					OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SAcknowledgeOrigins());
				}
			});
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return null;
	}
}
