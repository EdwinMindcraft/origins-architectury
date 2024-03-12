package io.github.apace100.origins.mixin;


import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.edwinmindcraft.apoli.api.component.PowerContainer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class ScareCreepersMixin extends Monster {
	protected ScareCreepersMixin(EntityType<? extends Monster> entityType, Level world) {
		super(entityType, world);
	}

	@Inject(at = @At("TAIL"), method = "registerGoals")
	private void addGoals(CallbackInfo info) {
		Goal goal = new AvoidEntityGoal<>(this, Player.class, e -> PowerContainer.hasPower(e, OriginsPowerTypes.SCARE_CREEPERS.get()), 6.0F, 1.0D, 1.2D, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
		this.goalSelector.addGoal(3, goal);
	}

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V", ordinal = 8), method = "registerGoals")
	private void redirectTargetGoal(GoalSelector goalSelector, int priority, Goal goal) {
		Goal newGoal = new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !PowerContainer.hasPower(e, OriginsPowerTypes.SCARE_CREEPERS.get()));
		goalSelector.addGoal(priority, newGoal);
	}
}