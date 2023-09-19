package io.github.edwinmindcraft.origins.common.network;

import com.google.common.collect.Sets;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.registry.ApoliDynamicRegistries;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import io.github.edwinmindcraft.origins.client.OriginsClientUtils;
import io.github.edwinmindcraft.origins.client.screen.WaitForPowersScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

public record S2COpenWaitingForPowersScreen(boolean isOrb, Set<ResourceKey<ConfiguredPower<?, ?>>> nonReadyPowers) {

	public static S2COpenWaitingForPowersScreen decode(FriendlyByteBuf buf) {
        boolean isOrb = buf.readBoolean();
        Set<ResourceKey<ConfiguredPower<?, ?>>> nonReadyPowers = Sets.newHashSet();
        int nonReadyPowerCount = buf.readInt();
        for (int i = 0; i < nonReadyPowerCount; ++i) {
            nonReadyPowers.add(buf.readResourceKey(ApoliDynamicRegistries.CONFIGURED_POWER_KEY));
        }
		return new S2COpenWaitingForPowersScreen(isOrb, nonReadyPowers);
	}

	public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isOrb());
        buf.writeInt(this.nonReadyPowers().size());
        for (ResourceKey<ConfiguredPower<?, ?>> key : this.nonReadyPowers()) {
            buf.writeResourceKey(key);
        }
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			Player player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> OriginsClientUtils::getClientPlayer);
			if (player == null) return;
			IOriginContainer.get(player).ifPresent(x -> {
                if (!this.nonReadyPowers().isEmpty()) {
                    Minecraft.getInstance().setScreen(new WaitForPowersScreen(OriginsClient.SHOW_DIRT_BACKGROUND, this.nonReadyPowers(), this.isOrb()));
                }
            });
		});
		contextSupplier.get().setPacketHandled(true);
	}
}
