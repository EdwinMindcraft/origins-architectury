package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConduitBlockEntity.class)
public class ConduitOnLandMixin {

	@Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
	private static boolean allowConduitPowerOnLand(Player playerEntity) {
		return playerEntity.isInWaterOrRain() || PowerContainer.hasPower(playerEntity, OriginsPowerTypes.CONDUIT_POWER_ON_LAND.get());
	}
}