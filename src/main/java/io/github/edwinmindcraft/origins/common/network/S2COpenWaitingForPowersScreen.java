package io.github.edwinmindcraft.origins.common.network;

import com.google.common.collect.Sets;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import io.github.edwinmindcraft.origins.client.OriginsClientUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;

public record S2COpenWaitingForPowersScreen(boolean isOrb, Set<ResourceKey<ConfiguredPower<?, ?>>> nonReadyPowers) implements CustomPacketPayload {
    public static final ResourceLocation ID = Origins.identifier("open_waiting_for_powers_screen");
    public static final Type<S2COpenWaitingForPowersScreen> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenWaitingForPowersScreen> STREAM_CODEC = StreamCodec.of(S2COpenWaitingForPowersScreen::encode, S2COpenWaitingForPowersScreen::decode);

	public static S2COpenWaitingForPowersScreen decode(FriendlyByteBuf buf) {
        boolean isOrb = buf.readBoolean();
        Set<ResourceKey<ConfiguredPower<?, ?>>> nonReadyPowers = Sets.newHashSet();
        int nonReadyPowerCount = buf.readInt();
        for (int i = 0; i < nonReadyPowerCount; ++i) {
            nonReadyPowers.add(buf.readResourceKey(ApoliDynamicRegistries.CONFIGURED_POWER_KEY));
        }
		return new S2COpenWaitingForPowersScreen(isOrb, nonReadyPowers);
	}

	public static void encode(FriendlyByteBuf buf, S2COpenWaitingForPowersScreen packet) {
        buf.writeBoolean(packet.isOrb());
        buf.writeInt(packet.nonReadyPowers().size());
        for (ResourceKey<ConfiguredPower<?, ?>> key : packet.nonReadyPowers()) {
            buf.writeResourceKey(key);
        }
	}

	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = OriginsClientUtils.getClientPlayer();
            if (player == null) return;
			// FIXME: OriginContainer.
            IOriginContainer.get(player).ifPresent(x -> {
                if (!this.nonReadyPowers().isEmpty()) {
                    OriginsClient.WAITING_FOR_POWERS.set(true);
                    OriginsClient.WAITING_POWERS.addAll(this.nonReadyPowers());
                    OriginsClient.SELECTION_WAS_ORB = this.isOrb();
                }
            });
		});
	}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
