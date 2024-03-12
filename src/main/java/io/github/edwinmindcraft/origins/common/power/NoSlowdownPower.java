package io.github.edwinmindcraft.origins.common.power;

import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.origins.common.power.configuration.NoSlowdownConfiguration;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public class NoSlowdownPower extends PowerFactory<NoSlowdownConfiguration> {
	public static boolean isActive(Entity player, BlockState state) {
		return PowerContainer.getPowers(player, OriginsPowerTypes.NO_SLOWDOWN.get()).stream().anyMatch(x -> x.value().getConfiguration().test(state));
	}


	public NoSlowdownPower() {
		super(NoSlowdownConfiguration.CODEC);
	}
}
