package io.github.apace100.origins.entity;

import io.github.apace100.origins.registry.ModEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class EnderianPearlEntity extends ThrowableItemProjectile {
   public EnderianPearlEntity(EntityType<? extends EnderianPearlEntity> entityType, Level world) {
      super(entityType, world);
   }

   public EnderianPearlEntity(Level world, LivingEntity owner) {
      super(ModEntities.ENDERIAN_PEARL, owner, world);
   }

   @Environment(EnvType.CLIENT)
   public EnderianPearlEntity(Level world, double x, double y, double z) {
      super(ModEntities.ENDERIAN_PEARL, x, y, z, world);
   }

   protected Item getDefaultItem() {
      return Items.ENDER_PEARL;
   }

   protected void onHit(HitResult hitResult) {
      super.onHit(hitResult);
      Entity entity = this.getOwner();

      for(int i = 0; i < 32; ++i) {
         this.level.addParticle(ParticleTypes.PORTAL, this.getX(), this.getY() + this.random.nextDouble() * 2.0D, this.getZ(), this.random.nextGaussian(), 0.0D, this.random.nextGaussian());
      }

      if (!this.level.isClientSide && !this.isRemoved()) {
         if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayerEntity = (ServerPlayer)entity;
            if (serverPlayerEntity.connection.getConnection().isConnected() && serverPlayerEntity.level == this.level && !serverPlayerEntity.isSleeping()) {

               if (entity.isPassenger()) {
                  entity.stopRiding();
               }

               entity.teleportTo(this.getX(), this.getY(), this.getZ());
               entity.fallDistance = 0.0F;
            }
         } else if (entity != null) {
            entity.teleportTo(this.getX(), this.getY(), this.getZ());
            entity.fallDistance = 0.0F;
         }

         this.discard();
      }

   }

   public void tick() {
      Entity entity = this.getOwner();
      if (entity instanceof Player && !entity.isAlive()) {
         this.discard();
      } else {
         super.tick();
      }

   }

   public Entity changeDimension(ServerLevel destination) {
      Entity entity = this.getOwner();
      if (entity != null && entity.level.dimension() != destination.dimension()) {
         this.setOwner((Entity)null);
      }

      return super.changeDimension(destination);
   }
}
