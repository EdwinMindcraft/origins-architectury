package io.github.apace100.origins.mixin.forge;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.registry.ModDamageSources;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CommonHooks.class)
public class CommonHooksMixin {
    @ModifyArg(method = "onLivingBreathe", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static DamageSource origins$modifyDrowningDamage(DamageSource source, @Local(argsOnly = true) LivingEntity entity) {
        if (PowerContainer.hasPower(entity, OriginsPowerTypes.WATER_BREATHING.get())) {
            return entity.damageSources().source(ModDamageSources.NO_WATER_FOR_GILLS);
        }
        return source;
    }
}
