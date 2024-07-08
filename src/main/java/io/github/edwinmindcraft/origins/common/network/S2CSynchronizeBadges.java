package io.github.edwinmindcraft.origins.common.network;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.Badge;
import io.github.apace100.origins.badge.BadgeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collection;
import java.util.Map;

public record S2CSynchronizeBadges(Multimap<ResourceLocation, Badge> badges) implements CustomPacketPayload {
	public static final ResourceLocation ID = Origins.identifier("sync_badges");
	public static final Type<S2CSynchronizeBadges> TYPE = new Type<>(ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, S2CSynchronizeBadges> STREAM_CODEC = StreamCodec.of(S2CSynchronizeBadges::encode, S2CSynchronizeBadges::decode);


	public static S2CSynchronizeBadges decode(FriendlyByteBuf buf) {
		Multimap<ResourceLocation, Badge> badges = LinkedListMultimap.create();
		int size = buf.readVarInt();
		for (int i = 0; i < size; i++) {
			ResourceLocation rl = buf.readResourceLocation();
			int count = buf.readVarInt();
			for (int j = 0; j < count; j++)
				badges.put(rl, BadgeManager.REGISTRY.receiveDataObject(buf));
		}
		return new S2CSynchronizeBadges(badges);
	}

	public static void encode(FriendlyByteBuf buf, S2CSynchronizeBadges packet) {
		Map<ResourceLocation, Collection<Badge>> map = packet.badges().asMap();
		buf.writeVarInt(map.size());
		for (Map.Entry<ResourceLocation, Collection<Badge>> entry : map.entrySet()) {
			buf.writeResourceLocation(entry.getKey());
			buf.writeVarInt(entry.getValue().size());
			for (Badge badge : entry.getValue())
				BadgeManager.REGISTRY.writeDataObject(buf, badge);
		}
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			BadgeManager.clear();
			this.badges.forEach(BadgeManager::putPowerBadge);
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
