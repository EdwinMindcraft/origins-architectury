package io.github.apace100.origins.networking;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.origin.OriginRegistry;
import io.github.apace100.origins.registry.ModComponents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import java.util.List;
import java.util.Random;

public class ModPacketsC2S {

    public static void register() {
        if(Origins.config.performVersionCheck) {
            ServerLoginConnectionEvents.QUERY_START.register(ModPacketsC2S::handshake);
            ServerLoginNetworking.registerGlobalReceiver(ModPackets.HANDSHAKE, ModPacketsC2S::handleHandshakeReply);
        }
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CHOOSE_ORIGIN, ModPacketsC2S::chooseOrigin);
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CHOOSE_RANDOM_ORIGIN, ModPacketsC2S::chooseRandomOrigin);
    }

    private static void chooseOrigin(MinecraftServer minecraftServer, ServerPlayer playerEntity, ServerGamePacketListenerImpl serverPlayNetworkHandler, FriendlyByteBuf packetByteBuf, PacketSender packetSender) {
        String originId = packetByteBuf.readUtf(32767);
        String layerId = packetByteBuf.readUtf(32767);
        minecraftServer.execute(() -> {
            OriginComponent component = ModComponents.ORIGIN.get(playerEntity);
            OriginLayer layer = OriginLayers.getLayer(ResourceLocation.tryParse(layerId));
            if(!component.hasAllOrigins() && !component.hasOrigin(layer)) {
                ResourceLocation id = ResourceLocation.tryParse(originId);
                if(id != null) {
                    Origin origin = OriginRegistry.get(id);
                    if(origin.isChoosable() && layer.contains(origin, playerEntity)) {
                        boolean hadOriginBefore = component.hadOriginBefore();
                        boolean hadAllOrigins = component.hasAllOrigins();
                        component.setOrigin(layer, origin);
                        component.checkAutoChoosingLayers(playerEntity, false);
                        component.sync();
                        if(component.hasAllOrigins() && !hadAllOrigins) {
                            OriginComponent.onChosen(playerEntity, hadOriginBefore);
                        }
                        Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " chose Origin: " + originId + ", for layer: " + layerId);
                    } else {
                        Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " tried to choose unchoosable Origin for layer " + layerId + ": " + originId + ".");
                        component.setOrigin(layer, Origin.EMPTY);
                    }
                    confirmOrigin(playerEntity, layer, component.getOrigin(layer));
                    component.sync();
                } else {
                    Origins.LOGGER.warn("Player " + playerEntity.getDisplayName().getContents() + " chose unknown origin: " + originId);
                }
            } else {
                Origins.LOGGER.warn("Player " + playerEntity.getDisplayName().getContents() + " tried to choose origin for layer " + layerId + " while having one already.");
            }
        });
    }

    private static void chooseRandomOrigin(MinecraftServer minecraftServer, ServerPlayer playerEntity, ServerGamePacketListenerImpl serverPlayNetworkHandler, FriendlyByteBuf packetByteBuf, PacketSender packetSender) {
        String layerId = packetByteBuf.readUtf(32767);
        minecraftServer.execute(() -> {
            OriginComponent component = ModComponents.ORIGIN.get(playerEntity);
            OriginLayer layer = OriginLayers.getLayer(ResourceLocation.tryParse(layerId));
            if(!component.hasAllOrigins() && !component.hasOrigin(layer)) {
                List<ResourceLocation> randomOrigins = layer.getRandomOrigins(playerEntity);
                if(layer.isRandomAllowed() && randomOrigins.size() > 0) {
                    ResourceLocation randomOrigin = randomOrigins.get(new Random().nextInt(randomOrigins.size()));
                    Origin origin = OriginRegistry.get(randomOrigin);
                    boolean hadOriginBefore = component.hadOriginBefore();
                    boolean hadAllOrigins = component.hasAllOrigins();
                    component.setOrigin(layer, origin);
                    component.checkAutoChoosingLayers(playerEntity, false);
                    component.sync();
                    if(component.hasAllOrigins() && !hadAllOrigins) {
                        OriginComponent.onChosen(playerEntity, hadOriginBefore);
                    }
                    Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " was randomly assigned the following Origin: " + randomOrigin + ", for layer: " + layerId);
                } else {
                    Origins.LOGGER.info("Player " + playerEntity.getDisplayName().getContents() + " tried to choose a random Origin for layer " + layerId + ", which is not allowed!");
                    component.setOrigin(layer, Origin.EMPTY);
                }
                confirmOrigin(playerEntity, layer, component.getOrigin(layer));
                component.sync();
            } else {
                Origins.LOGGER.warn("Player " + playerEntity.getDisplayName().getContents() + " tried to choose origin for layer " + layerId + " while having one already.");
            }
        });
    }

    private static void handleHandshakeReply(MinecraftServer minecraftServer, ServerLoginPacketListenerImpl serverLoginNetworkHandler, boolean understood, FriendlyByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender) {
        if (understood) {
            int clientSemVerLength = packetByteBuf.readInt();
            int[] clientSemVer = new int[clientSemVerLength];
            boolean mismatch = clientSemVerLength != Origins.SEMVER.length;
            for(int i = 0; i < clientSemVerLength; i++) {
                clientSemVer[i] = packetByteBuf.readInt();
                if(i < clientSemVerLength - 1 && clientSemVer[i] != Origins.SEMVER[i]) {
                    mismatch = true;
                }
            }
            if(mismatch) {
                StringBuilder clientVersionString = new StringBuilder();
                for(int i = 0; i < clientSemVerLength; i++) {
                    clientVersionString.append(clientSemVer[i]);
                    if(i < clientSemVerLength - 1) {
                        clientVersionString.append(".");
                    }
                }
                serverLoginNetworkHandler.disconnect(new TranslatableComponent("origins.gui.version_mismatch", Origins.VERSION, clientVersionString));
            }
        } else {
            serverLoginNetworkHandler.disconnect(new TextComponent("This server requires you to install the Origins mod (v" + Origins.VERSION + ") to play."));
        }
    }

    private static void handshake(ServerLoginPacketListenerImpl serverLoginNetworkHandler, MinecraftServer minecraftServer, PacketSender packetSender, ServerLoginNetworking.LoginSynchronizer loginSynchronizer) {
        packetSender.sendPacket(ModPackets.HANDSHAKE, PacketByteBufs.empty());
    }

    private static void confirmOrigin(ServerPlayer player, OriginLayer layer, Origin origin) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeResourceLocation(layer.getIdentifier());
        buf.writeResourceLocation(origin.getIdentifier());
        ServerPlayNetworking.send(player, ModPackets.CONFIRM_ORIGIN, buf);
    }
}
