package io.github.apace100.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class OriginCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				literal("origin").requires(cs -> cs.hasPermission(2))
						.then(literal("set")
								.then(argument("targets", EntityArgument.players())
										.then(argument("layer", LayerArgumentType.layer())
												.then(argument("origin", OriginArgumentType.origin())
														.executes((command) -> {
															// Sets the origins of several people in the given layer.
															int i = 0;
															Collection<ServerPlayer> targets = EntityArgument.getPlayers(command, "targets");
															ResourceKey<OriginLayer> l = LayerArgumentType.getLayer(command, "layer");
															ResourceKey<Origin> o = OriginArgumentType.getOrigin(command, "origin");
															for (ServerPlayer target : targets) {
																setOrigin(target, l, o);
																i++;
															}
															if (targets.size() == 1)
																command.getSource().sendSuccess(Component.translatable("commands.origin.set.success.single", targets.iterator().next().getDisplayName(), l.location(), o.location()), true);
															else
																command.getSource().sendSuccess(Component.translatable("commands.origin.set.success.multiple", targets.size(), l.location(), o.location()), true);
															return i;
														}))))
						)
						.then(literal("has")
								.then(argument("targets", EntityArgument.players())
										.then(argument("layer", LayerArgumentType.layer())
												.then(argument("origin", OriginArgumentType.origin())
														.executes((command) -> {
															// Returns the number of people in the target selector with the origin in the given layer.
															// Useful for checking if a player has the given origin in functions.
															int i = 0;
															Collection<ServerPlayer> targets = EntityArgument.getPlayers(command, "targets");
															ResourceKey<OriginLayer> l = LayerArgumentType.getLayer(command, "layer");
															ResourceKey<Origin> o = OriginArgumentType.getOrigin(command, "origin");
															for (ServerPlayer target : targets) {
																if (hasOrigin(target, l, o)) {
																	i++;
																}
															}
															if (i == 0) {
																command.getSource().sendFailure(Component.translatable("commands.execute.conditional.fail"));
															} else if (targets.size() == 1) {
																command.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
															} else {
																command.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass_count", i), false);
															}
															return i;
														}))))
						)
						.then(literal("get")
								.then(argument("target", EntityArgument.player())
										.then(argument("layer", LayerArgumentType.layer())
												.executes((command) -> {
													ServerPlayer target = EntityArgument.getPlayer(command, "target");
													ResourceKey<OriginLayer> layer = LayerArgumentType.getLayer(command, "layer");
													IOriginContainer.get(target).ifPresent(container -> {
														ResourceKey<Origin> origin = container.getOrigin(layer);
														command.getSource().sendSuccess(Component.translatable("commands.origin.get.result", target.getDisplayName(), layer.location(), origin.location(), origin.location()), false);
													});
													return 1;
												})
										)
								)
						).then(literal("gui")
								.then(argument("targets", EntityArgument.players())
										.executes((command) -> {
											Collection<ServerPlayer> targets = EntityArgument.getPlayers(command, "targets");
											targets.forEach(target -> IOriginContainer.get(target).ifPresent(container -> {
												OriginsAPI.getActiveLayers().forEach(x -> container.setOrigin(x, OriginRegisters.EMPTY.getHolder().orElseThrow()));
												container.synchronize();
												container.checkAutoChoosingLayers(false);
												OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new S2COpenOriginScreen(false));
											}));
											command.getSource().sendSuccess(Component.translatable("commands.origin.gui.all", targets.size()), false);
											return targets.size();
										})
										.then(argument("layer", LayerArgumentType.layer())
												.executes((command) -> {
													ResourceKey<OriginLayer> layer = LayerArgumentType.getLayer(command, "layer");
													Collection<ServerPlayer> targets = EntityArgument.getPlayers(command, "targets");
													Registry<OriginLayer> layersRegistry = OriginsAPI.getLayersRegistry();
													targets.forEach(target -> IOriginContainer.get(target).ifPresent(container -> {
														OriginLayer originLayer = layersRegistry.get(layer);
														if (originLayer == null) return;
														if (originLayer.enabled())
															container.setOrigin(layer, Objects.requireNonNull(OriginRegisters.EMPTY.getKey()));
														container.synchronize();
														container.checkAutoChoosingLayers(false);
														OriginsCommon.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new S2COpenOriginScreen(false));
													}));
													command.getSource().sendSuccess(Component.translatable("commands.origin.gui.layer", targets.size(), layer.location()), false);
													return targets.size();
												})
										)
								)
						)
		);
	}

	private static void setOrigin(Player player, ResourceKey<OriginLayer> layer, ResourceKey<Origin> origin) {
		IOriginContainer.get(player).ifPresent(container -> {
					container.setOrigin(layer, origin);
					container.synchronize();
					container.onChosen(origin, container.hadAllOrigins());
				}
		);
	}

	private static boolean hasOrigin(Player player, ResourceKey<OriginLayer> layer, ResourceKey<Origin> origin) {
		return IOriginContainer.get(player).map(x -> Objects.equals(x.getOrigin(layer), origin)).orElse(false);
	}
}
