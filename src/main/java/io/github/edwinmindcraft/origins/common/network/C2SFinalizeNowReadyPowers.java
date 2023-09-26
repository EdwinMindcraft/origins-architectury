package io.github.edwinmindcraft.origins.common.network;

import com.google.common.collect.Sets;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.origins.api.origin.IOriginCallbackPower;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Supplier;

public record C2SFinalizeNowReadyPowers(Set<ResourceKey<ConfiguredPower<?, ?>>> keys, boolean wasOrb) {
	public static C2SFinalizeNowReadyPowers decode(FriendlyByteBuf buf) {
        int keySize = buf.readInt();
        Set<ResourceKey<ConfiguredPower<?, ?>>> keys = Sets.newHashSet();
        for (int i = 0; i < keySize; ++i) {
            keys.add(buf.readResourceKey(ApoliDynamicRegistries.CONFIGURED_POWER_KEY));
        }
        boolean wasOrb = buf.readBoolean();
		return new C2SFinalizeNowReadyPowers(keys, wasOrb);
	}

	public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.keys().size());
        for (ResourceKey<ConfiguredPower<?, ?>> key : this.keys()) {
            buf.writeResourceKey(key);
        }
        buf.writeBoolean(this.wasOrb());
	}

    @SuppressWarnings({"rawtypes", "unchecked"})
	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			ServerPlayer sender = contextSupplier.get().getSender();
			if (sender == null) return;
            for (ResourceKey<ConfiguredPower<?, ?>> key : this.keys()) {
                @Nullable Holder<ConfiguredPower<?, ?>> configuredPower = (Holder<ConfiguredPower<?,?>>)(Object)ApoliAPI.getPowerContainer(sender).getPower(key);
                if (configuredPower != null && configuredPower.isBound() && configuredPower.value().getFactory() instanceof IOriginCallbackPower callbackPower) {
                    callbackPower.onChosen(configuredPower.value(), sender, wasOrb());
                }
            }
		});
		contextSupplier.get().setPacketHandled(true);
	}
}
