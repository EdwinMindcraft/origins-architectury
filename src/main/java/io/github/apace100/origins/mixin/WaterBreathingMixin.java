package io.github.apace100.origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public final class WaterBreathingMixin {

	/**
	 * Moved to {@link io.github.edwinmindcraft.origins.common.OriginsEventHandler#modifyBreathing(LivingBreatheEvent)}
	 */
	/*
	@Mixin(LivingEntity.class)
	public static abstract class CanBreatheInWater extends Entity {

		public CanBreatheInWater(EntityType<?> type, Level world) {
			super(type, world);
		}

		@Inject(at = @At("HEAD"), method = "canBreatheUnderwater", cancellable = true)
		public void doWaterBreathing(CallbackInfoReturnable<Boolean> info) {
			if (PowerContainer.hasPower(this, OriginsPowerTypes.WATER_BREATHING.get()))
				info.setReturnValue(true);
		}
	}
	 */

	@Mixin(Player.class)
	public static abstract class UpdateAir extends LivingEntity {

		protected UpdateAir(EntityType<? extends LivingEntity> entityType, Level world) {
			super(entityType, world);
		}

		@ModifyExpressionValue(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z"), method = "turtleHelmetTick")
		public boolean isSubmergedInProxy(boolean original) {
			return PowerContainer.hasPower(this, OriginsPowerTypes.WATER_BREATHING.get()) != original;
		}
	}
}
