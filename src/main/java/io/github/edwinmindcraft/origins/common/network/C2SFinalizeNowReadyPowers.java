package io.github.edwinmindcraft.origins.common.network;

import com.google.common.collect.Sets;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.origins.api.origin.IOriginCallbackPower;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.Set;

public record C2SFinalizeNowReadyPowers(Set<ResourceKey<ConfiguredPower<?, ?>>> keys, boolean wasOrb) implements CustomPacketPayload {
    public static final ResourceLocation ID = ApoliAPI.identifier("finalize_ready_powers");
    public static final Type<C2SFinalizeNowReadyPowers> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SFinalizeNowReadyPowers> STREAM_CODEC = StreamCodec.of(C2SFinalizeNowReadyPowers::encode, C2SFinalizeNowReadyPowers::decode);


    public static C2SFinalizeNowReadyPowers decode(FriendlyByteBuf buf) {
        int keySize = buf.readInt();
        Set<ResourceKey<ConfiguredPower<?, ?>>> keys = Sets.newHashSet();
        for (int i = 0; i < keySize; ++i) {
            keys.add(buf.readResourceKey(ApoliDynamicRegistries.CONFIGURED_POWER_KEY));
        }
        boolean wasOrb = buf.readBoolean();
		return new C2SFinalizeNowReadyPowers(keys, wasOrb);
	}

	public static void encode(FriendlyByteBuf buf, C2SFinalizeNowReadyPowers packet) {
        buf.writeInt(packet.keys().size());
        for (ResourceKey<ConfiguredPower<?, ?>> key : packet.keys()) {
            buf.writeResourceKey(key);
        }
        buf.writeBoolean(packet.wasOrb());
	}

    @SuppressWarnings({"rawtypes", "unchecked"})
	public void handle(IPayloadContext context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = (ServerPlayer) context.player();
            for (ResourceKey<ConfiguredPower<?, ?>> key : this.keys()) {
                @Nullable Holder<ConfiguredPower<?, ?>> configuredPower = (Holder<ConfiguredPower<?,?>>)(Object)ApoliAPI.getPowerContainer(sender).getPower(key);
                if (configuredPower != null && configuredPower.isBound() && configuredPower.value().getFactory() instanceof IOriginCallbackPower callbackPower) {
                    callbackPower.onChosen(configuredPower.value(), sender, wasOrb());
                }
            }
		});
	}

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
