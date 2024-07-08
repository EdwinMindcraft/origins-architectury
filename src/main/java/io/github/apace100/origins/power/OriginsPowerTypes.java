package io.github.apace100.origins.power;

import io.github.edwinmindcraft.apoli.common.power.DummyPower;
import io.github.edwinmindcraft.origins.common.power.NoSlowdownPower;
import io.github.edwinmindcraft.origins.common.power.WaterVisionPower;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;

import java.util.function.Supplier;

public class OriginsPowerTypes {

	public static final Supplier<DummyPower> LIKE_WATER = OriginRegisters.POWER_FACTORIES.register("like_water", DummyPower::new);
	public static final Supplier<DummyPower> WATER_BREATHING = OriginRegisters.POWER_FACTORIES.register("water_breathing", DummyPower::new);
	public static final Supplier<DummyPower> SCARE_CREEPERS = OriginRegisters.POWER_FACTORIES.register("scare_creepers", DummyPower::new);
	public static final Supplier<WaterVisionPower> WATER_VISION = OriginRegisters.POWER_FACTORIES.register("water_vision", WaterVisionPower::new);
	public static final Supplier<NoSlowdownPower> NO_SLOWDOWN = OriginRegisters.POWER_FACTORIES.register("no_slowdown", NoSlowdownPower::new);
	public static final Supplier<DummyPower> CONDUIT_POWER_ON_LAND = OriginRegisters.POWER_FACTORIES.register("conduit_power_on_land", DummyPower::new);
	public static final Supplier<OriginsCallbackPower> ACTION_ON_CALLBACK = OriginRegisters.POWER_FACTORIES.register("action_on_callback", OriginsCallbackPower::new);

	public static void register() {}
}
