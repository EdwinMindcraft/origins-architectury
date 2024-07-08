package io.github.edwinmindcraft.origins.common.power;

import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.origins.common.power.configuration.WaterVisionConfiguration;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public class WaterVisionPower extends PowerFactory<WaterVisionConfiguration> {
	public static Optional<Float> getWaterVisionStrength(LivingEntity living) {
		if (!PowerContainer.hasPower(living, OriginsPowerTypes.WATER_VISION.get()))
			return Optional.empty();
		return PowerContainer.getPowers(living, OriginsPowerTypes.WATER_VISION.get()).stream().map(x -> x.value().getConfiguration().strength()).max(Float::compareTo);
	}

	public WaterVisionPower() {
		super(WaterVisionConfiguration.CODEC);
	}
}
