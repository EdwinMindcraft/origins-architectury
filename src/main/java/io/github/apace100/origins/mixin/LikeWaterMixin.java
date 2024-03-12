package io.github.apace100.origins.mixin;

import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LikeWaterMixin extends Entity {

    public LikeWaterMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getFluidFallingAdjustedMovement(DZLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 method_26317Proxy(LivingEntity entity, double d, boolean bl, Vec3 vec3d) {
        Vec3 oldReturn = entity.getFluidFallingAdjustedMovement(d, bl, vec3d);
        if(PowerContainer.hasPower(entity, OriginsPowerTypes.LIKE_WATER.get())) {
            if (Math.abs(vec3d.y - d / 16.0D) < 0.025D) {
                return new Vec3(oldReturn.x, 0, oldReturn.z);
            }
        }
        return entity.getFluidFallingAdjustedMovement(d, bl, vec3d);
    }
}
