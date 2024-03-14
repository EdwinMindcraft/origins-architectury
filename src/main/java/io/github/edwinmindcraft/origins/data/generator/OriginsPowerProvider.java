package io.github.edwinmindcraft.origins.data.generator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.apace100.apoli.util.AttributedEntityAttributeModifier;
import io.github.apace100.apoli.util.Comparison;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.power.OriginsPowerTypes;
import io.github.apace100.origins.registry.ModDamageSources;
import io.github.apace100.origins.registry.ModEnchantments;
import io.github.edwinmindcraft.apoli.api.configuration.*;
import io.github.edwinmindcraft.apoli.api.generator.PowerGenerator;
import io.github.edwinmindcraft.apoli.api.power.ConditionData;
import io.github.edwinmindcraft.apoli.api.power.IActivePower;
import io.github.edwinmindcraft.apoli.api.power.PowerData;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBlockCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredItemCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.TogglePowerConfiguration;
import io.github.edwinmindcraft.apoli.api.registry.ApoliBuiltinRegistries;
import io.github.edwinmindcraft.apoli.common.action.configuration.DamageConfiguration;
import io.github.edwinmindcraft.apoli.common.action.configuration.GiveConfiguration;
import io.github.edwinmindcraft.apoli.common.action.configuration.PlaySoundConfiguration;
import io.github.edwinmindcraft.apoli.common.condition.configuration.BlockCollisionConfiguration;
import io.github.edwinmindcraft.apoli.common.condition.configuration.EnchantmentConfiguration;
import io.github.edwinmindcraft.apoli.common.condition.configuration.FluidTagComparisonConfiguration;
import io.github.edwinmindcraft.apoli.common.condition.configuration.ProjectileConfiguration;
import io.github.edwinmindcraft.apoli.common.condition.meta.ConditionStreamConfiguration;
import io.github.edwinmindcraft.apoli.common.power.configuration.*;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
import io.github.edwinmindcraft.apoli.common.registry.action.ApoliEntityActions;
import io.github.edwinmindcraft.apoli.common.registry.condition.*;
import io.github.edwinmindcraft.origins.common.power.configuration.NoSlowdownConfiguration;
import io.github.edwinmindcraft.origins.common.power.configuration.WaterVisionConfiguration;
import io.github.edwinmindcraft.origins.data.tag.OriginsBlockTags;
import io.github.edwinmindcraft.origins.data.tag.OriginsItemTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.DataGenerator;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OriginsPowerProvider extends PowerGenerator {
	public OriginsPowerProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
		super(generator, Origins.MODID, existingFileHelper);
	}

	private static Map<String, Holder<ConfiguredPower<?, ?>>> makeAquaAffinity() {
		ImmutableMap.Builder<String, Holder<ConfiguredPower<?, ?>>> builder = ImmutableMap.builder();
		Holder<ConfiguredBlockCondition<?, ?>> allow = ApoliDefaultConditions.BLOCK_DEFAULT.getHolder().orElseThrow(RuntimeException::new);
		builder.put("underwater", Holder.direct(
				ApoliPowers.MODIFY_BREAK_SPEED.get()
						.configure(
								new ModifyValueBlockConfiguration(ListConfiguration.of(ModifierUtil.fromAttributeModifier(new AttributeModifier(UUID.randomUUID(), "Unnamed attribute modifier", 4, AttributeModifier.Operation.MULTIPLY_TOTAL))), allow),
								PowerData.builder().addCondition(ApoliEntityConditions.and(
										ApoliEntityConditions.SUBMERGED_IN.get().configure(new TagConfiguration<>(FluidTags.WATER)),
										ApoliEntityConditions.ENCHANTMENT.get().configure(new EnchantmentConfiguration(new IntegerComparisonConfiguration(Comparison.EQUAL, 0), Optional.of(Enchantments.AQUA_AFFINITY), EnchantmentConfiguration.Calculation.SUM))
								)).build())));
		builder.put("ungrounded", Holder.direct(
				ApoliPowers.MODIFY_BREAK_SPEED.get()
						.configure(
								new ModifyValueBlockConfiguration(ListConfiguration.of(ModifierUtil.fromAttributeModifier(new AttributeModifier(UUID.randomUUID(), "Unnamed attribute modifier", 4, AttributeModifier.Operation.MULTIPLY_TOTAL))), allow),
								PowerData.builder().addCondition(ApoliEntityConditions.and(
										ApoliEntityConditions.FLUID_HEIGHT.get().configure(new FluidTagComparisonConfiguration(new DoubleComparisonConfiguration(Comparison.GREATER_THAN, 0), FluidTags.WATER)),
										ApoliEntityConditions.ON_BLOCK.get().configure(HolderConfiguration.defaultCondition(ApoliBuiltinRegistries.CONFIGURED_BLOCK_CONDITIONS), new ConditionData(true))
								)).build())));
		return builder.build();
	}

	/*private static Map<String, ConfiguredPower<?, ?>> makeMasterOfWebs() {
		ConfiguredBlockCondition<?, ?> inCobwebs = ApoliBlockConditions.IN_TAG.get().configure(new TagConfiguration<>(OriginsBlockTags.COBWEBS));

		ImmutableMap.Builder<String, ConfiguredPower<?, ?>> builder = ImmutableMap.builder();
		builder.put("webbing", ApoliPowers.TARGET_ACTION_ON_HIT.get().configure(
				new ConditionedCombatActionConfiguration(200, new HudRender(true, 5, Origins.identifier("textures/gui/resource_bar.png"), null, false), null, null,
						ApoliEntityActions.BLOCK_ACTION_AT.get().configure(FieldConfiguration.of(
								ApoliBlockActions.IF_ELSE.get().configure(new IfElseConfiguration<>(
										Holder.direct(ApoliBlockConditions.REPLACEABLE.get().configure(NoConfiguration.INSTANCE)),
										Holder.direct(ApoliBlockActions.SET_BLOCK.get().configure(new BlockConfiguration(ModBlocks.TEMPORARY_COBWEB.get()))),
										null, ApoliBlockActions.PREDICATE, ApoliBlockActions.EXECUTOR))
						))),
				PowerData.DEFAULT));
		builder.put("no_slowdown", OriginsPowerTypes.NO_SLOWDOWN.get().configure(new NoSlowdownConfiguration(OriginsBlockTags.COBWEBS), PowerData.DEFAULT));
		builder.put("climbing", ApoliPowers.CLIMBING.get().configure(
				new ClimbingConfiguration(true, ApoliEntityConditions.POWER_ACTIVE.get().configure(new PowerReference(Origins.identifier("master_of_webs_climbing")))),
				PowerData.builder().addCondition(ApoliEntityConditions.and(
						ApoliEntityConditions.IN_BLOCK_ANYWHERE.get().configure(new InBlockAnywhereConfiguration(inCobwebs)),
						ApoliEntityConditions.POWER_ACTIVE.get().configure(new PowerReference(Origins.identifier("climbing_toggle")))
				)).build()
		));
		builder.put("punch_through", ApoliPowers.PREVENT_BLOCK_SELECTION.get().configure(FieldConfiguration.of(Optional.of(inCobwebs)), PowerData.builder()
				.addCondition(ApoliEntityConditions.SNEAKING.get().configure(NoConfiguration.INSTANCE, new ConditionData(true))).build()));
		builder.put("sense", ApoliPowers.ENTITY_GLOW.get().configure(new EntityGlowConfiguration(ApoliEntityConditions.and(
				ApoliEntityConditions.IN_BLOCK_ANYWHERE.get().configure(new InBlockAnywhereConfiguration(inCobwebs)),
				ApoliEntityConditions.ENTITY_GROUP.get().configure(FieldConfiguration.of(MobType.ARTHROPOD), new ConditionData(true))
		)), PowerData.DEFAULT));
		//FIXME Recipe serialization is broken for now.
		builder.put("web_crafting", ApoliPowers.RECIPE.get().configure(FieldConfiguration.of(
				new ShapelessRecipe(Origins.identifier("master_of_webs/web_crafting"), "", Items.COBWEB.getDefaultInstance(), NonNullList.of(Ingredient.of(Items.STRING), Ingredient.of(Items.STRING)))
		), PowerData.DEFAULT));
		return builder.build();
	}*/

	private void makeArachnidPowers() {
		PowerData hidden = PowerData.builder().hidden().build();

		ConfiguredItemCondition<?, ?> carnivore = ApoliItemConditions.and(
				ApoliItemConditions.OR.get().configure(ConditionStreamConfiguration.or(ImmutableList.of(HolderSet.direct(Holder::direct, ImmutableList.of(ApoliItemConditions.INGREDIENT.get().configure(FieldConfiguration.of(Ingredient.of(OriginsItemTags.MEAT))), ApoliItemConditions.MEAT.get().configure(NoConfiguration.INSTANCE)))), ApoliItemConditions.PREDICATE), new ConditionData(true)),
				ApoliItemConditions.FOOD.get().configure(NoConfiguration.INSTANCE),
				ApoliItemConditions.INGREDIENT.get().configure(FieldConfiguration.of(Ingredient.of(OriginsItemTags.IGNORE_DIET)), new ConditionData(true)));

		this.add("arthropod", ApoliPowers.ENTITY_GROUP.get().configure(FieldConfiguration.of(MobType.ARTHROPOD), hidden));
		this.add("carnivore", ApoliPowers.PREVENT_ITEM_USAGE.get().configure(FieldConfiguration.of(Optional.of(carnivore)), PowerData.DEFAULT));
		this.add("climbing", ApoliPowers.multiple(
				ImmutableMap.of(
						"toggle", ApoliPowers.TOGGLE.get().configure(new TogglePowerConfiguration.Impl(true, IActivePower.Key.PRIMARY, false), PowerData.DEFAULT),
						"climbing", ApoliPowers.CLIMBING.get().configure(new ClimbingConfiguration(true, Holder.direct(ApoliEntityConditions.or(
								ApoliEntityConditions.BLOCK_COLLISION.get().configure(new BlockCollisionConfiguration(new Vec3(-0.01, 0, -0.01), ApoliDefaultConditions.BLOCK_DEFAULT.getHolder().orElseThrow())),
								ApoliEntityConditions.BLOCK_COLLISION.get().configure(new BlockCollisionConfiguration(new Vec3(0.01, 0, 0.01), ApoliDefaultConditions.BLOCK_DEFAULT.getHolder().orElseThrow()))
						))), PowerData.builder().addCondition(ApoliEntityConditions.and(
								ApoliEntityConditions.POWER_ACTIVE.get().configure(new PowerReference(Origins.identifier("climbing_toggle"))),
								ApoliEntityConditions.COLLIDED_HORIZONTALLY.get().configure(NoConfiguration.INSTANCE)
						)).build()))));
		//this.add("master_of_webs", ApoliPowers.MULTIPLE.get().configure(new MultipleConfiguration<>(makeMasterOfWebs()), PowerData.DEFAULT));
		this.add("fragile", ApoliPowers.ATTRIBUTE.get().configure(new AttributeConfiguration(new AttributedEntityAttributeModifier(Attributes.MAX_HEALTH, new AttributeModifier("Fragile health reduction", -6.0, AttributeModifier.Operation.ADDITION))), PowerData.DEFAULT));
	}

	private void makeAvianPowers() {
		ConfiguredItemCondition<?, ?> vegetarian = ApoliItemConditions.and(
				ApoliItemConditions.OR.get().configure(ConditionStreamConfiguration.or(ImmutableList.of(HolderSet.direct(Holder::direct, ImmutableList.of(ApoliItemConditions.INGREDIENT.get().configure(FieldConfiguration.of(Ingredient.of(OriginsItemTags.MEAT))), ApoliItemConditions.MEAT.get().configure(NoConfiguration.INSTANCE)))), ApoliItemConditions.PREDICATE)),
				ApoliItemConditions.FOOD.get().configure(NoConfiguration.INSTANCE),
				ApoliItemConditions.INGREDIENT.get().configure(FieldConfiguration.of(Ingredient.of(OriginsItemTags.IGNORE_DIET)), new ConditionData(true)));
		this.add("vegetarian", ApoliPowers.PREVENT_ITEM_USAGE.get().configure(FieldConfiguration.of(Optional.of(vegetarian)), PowerData.DEFAULT));
		this.add("tailwind", ApoliPowers.ATTRIBUTE.get().configure(new AttributeConfiguration(new AttributedEntityAttributeModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier("Tailwind speed bonus", 0.2, AttributeModifier.Operation.MULTIPLY_BASE))), PowerData.DEFAULT));
		this.add("lay_eggs", ApoliPowers.ACTION_ON_WAKE_UP.get().configure(new ActionOnWakeUpConfiguration(null,
				ApoliEntityActions.and(
						ApoliEntityActions.GIVE.get().configure(new GiveConfiguration(new ItemStack(Items.EGG, 1))),
						ApoliEntityActions.PLAY_SOUND.get().configure(new PlaySoundConfiguration(SoundEvents.CHICKEN_EGG, 1.0F, 1.0F))
				), null), PowerData.DEFAULT));
		this.add("slow_falling", ApoliPowers.MODIFY_FALLING.get().configure(new ModifyFallingConfiguration(Optional.of(0.01), false, ListConfiguration.of()),
				PowerData.builder().addCondition(ApoliEntityConditions.or(
						ApoliEntityConditions.and(ApoliEntityConditions.SNEAKING.get().configure(NoConfiguration.INSTANCE), ApoliEntityConditions.FALL_FLYING.get().configure(NoConfiguration.INSTANCE)),
						ApoliEntityConditions.and(ApoliEntityConditions.SNEAKING.get().configure(NoConfiguration.INSTANCE, new ConditionData(true)), ApoliEntityConditions.FALL_FLYING.get().configure(NoConfiguration.INSTANCE, new ConditionData(true)))
				)).build()));
		this.add("like_air", ApoliPowers.ATTRIBUTE_MODIFY_TRANSFER.get().configure(new AttributeModifyTransferConfiguration(ApoliPowers.MODIFY_AIR_SPEED.get(), Attributes.MOVEMENT_SPEED, 1.0), PowerData.DEFAULT));
		this.add("fresh_air", ApoliPowers.PREVENT_SLEEP.get().configure(new PreventSleepConfiguration(
				Holder.direct(ApoliBlockConditions.HEIGHT.get().configure(new IntegerComparisonConfiguration(Comparison.LESS_THAN, 86))),
				"origins.avian_sleep_fail",
				false
		), PowerData.DEFAULT));
	}

	private void makeBlazebornPowers() {
		PowerData hidden = PowerData.builder().hidden().build();
		this.add("burning_wrath", ApoliPowers.MODIFY_DAMAGE_DEALT.get().configure(
				new ModifyDamageDealtConfiguration(ModifierUtil.fromAttributeModifier(new AttributeModifier("Additional damage while on fire", 3, AttributeModifier.Operation.ADDITION))),
				PowerData.builder().addCondition(ApoliEntityConditions.ON_FIRE.get().configure(NoConfiguration.INSTANCE)).build()));
		this.add("damage_from_potions", ApoliPowers.ACTION_ON_ITEM_USE.get().configure(
				new ActionOnItemUseConfiguration(
						Holder.direct(ApoliItemConditions.INGREDIENT.get().configure(new FieldConfiguration<>(Ingredient.of(Items.POTION)))),
						Holder.direct(ApoliEntityActions.DAMAGE.get().configure(new DamageConfiguration(Optional.of(ModDamageSources.NO_WATER_FOR_GILLS), Optional.empty(), 2.0F))),
						ApoliDefaultActions.ITEM_DEFAULT.getHolder().orElseThrow(),
                        ActionOnItemUseConfiguration.TriggerType.FINISH,
                        0
				), hidden
		));
		this.add("damage_from_snowballs", ApoliPowers.MODIFY_DAMAGE_TAKEN.get().configure(
				new ModifyDamageTakenConfiguration(
						ListConfiguration.of(ModifierUtil.fromAttributeModifier(new AttributeModifier("Snowball damage taken like Blazes", 3, AttributeModifier.Operation.ADDITION))),
						Holder.direct(ApoliDamageConditions.PROJECTILE.get().configure(new ProjectileConfiguration(Optional.of(EntityType.SNOWBALL), ApoliDefaultConditions.ENTITY_DEFAULT.getHolder().orElseThrow()))),
						ApoliDefaultConditions.BIENTITY_DEFAULT.getHolder().orElseThrow(),
						ApoliDefaultActions.ENTITY_DEFAULT.getHolder().orElseThrow(),
						ApoliDefaultActions.ENTITY_DEFAULT.getHolder().orElseThrow(),
						ApoliDefaultActions.BIENTITY_DEFAULT.getHolder().orElseThrow(),
						ApoliDefaultConditions.ENTITY_DEFAULT.getHolder().orElseThrow(),
						ApoliDefaultConditions.ENTITY_DEFAULT.getHolder().orElseThrow()
				), hidden
		));
		this.add("fire_immunity", ApoliPowers.INVULNERABILITY.get().configure(HolderConfiguration.of(Holder.direct(ApoliDamageConditions.FIRE.get().configure(NoConfiguration.INSTANCE))), PowerData.DEFAULT));
		this.add("flame_particles", ApoliPowers.PARTICLE.get().configure(new ParticleConfiguration(ParticleTypes.FLAME, 4, false, new Vec3(0.25, 0.5, 0.25), 1.0F, 1, false, 0), hidden));
		this.add("hotblooded", ApoliPowers.EFFECT_IMMUNITY.get().configure(new EffectImmunityConfiguration(ListConfiguration.of(MobEffects.POISON, MobEffects.HUNGER), false), PowerData.DEFAULT));
		this.add("nether_spawn", ApoliPowers.MODIFY_PLAYER_SPAWN.get().configure(new ModifyPlayerSpawnConfiguration(Level.NETHER, 0.125F, null, ModifyPlayerSpawnConfiguration.SpawnStrategy.CENTER, null, null), PowerData.DEFAULT));
		this.add("water_vulnerability", ApoliPowers.DAMAGE_OVER_TIME.get().configure(
				new DamageOverTimeConfiguration(20, 1, 1, 2, ModDamageSources.NO_WATER_FOR_GILLS, null, ModEnchantments.WATER_PROTECTION.get(), 1.0F),
				PowerData.builder()
						.addCondition(ApoliEntityConditions.or(
								ApoliEntityConditions.FLUID_HEIGHT.get().configure(new FluidTagComparisonConfiguration(new DoubleComparisonConfiguration(Comparison.GREATER_THAN, 0.0), FluidTags.WATER)),
								ApoliEntityConditions.IN_RAIN.get().configure(NoConfiguration.INSTANCE)
						)).build()
		));
	}

	@Override
	protected void populate() {
		this.makeArachnidPowers();
		this.makeAvianPowers();
		this.makeBlazebornPowers();

		PowerData hidden = PowerData.builder().hidden().build();
		ConditionData inverted = new ConditionData(true);
		this.add("like_water", OriginsPowerTypes.LIKE_WATER.get().configure(NoConfiguration.INSTANCE, PowerData.DEFAULT));
		this.add("water_breathing", OriginsPowerTypes.WATER_BREATHING.get().configure(NoConfiguration.INSTANCE, PowerData.DEFAULT));
		this.add("scare_creepers", OriginsPowerTypes.SCARE_CREEPERS.get().configure(NoConfiguration.INSTANCE, PowerData.DEFAULT));
		this.add("water_vision", ApoliPowers.MULTIPLE.get().configure(new MultipleConfiguration<>(ImmutableMap.of(
						"vision", Holder.direct(OriginsPowerTypes.WATER_VISION.get().configure(new WaterVisionConfiguration(1.0F), PowerData.builder()
								.addCondition(ApoliEntityConditions.POWER_ACTIVE.get().configure(new PowerReference(Origins.identifier("water_vision_toggle")))).build())),
						"toggle", Holder.direct(ApoliPowers.TOGGLE_NIGHT_VISION.get().configure(new ToggleNightVisionConfiguration(true, IActivePower.Key.PRIMARY, 1.0F), PowerData.builder()
								.addCondition(ApoliEntityConditions.SUBMERGED_IN.get().configure(new TagConfiguration<>(FluidTags.WATER))).build())))),
				PowerData.DEFAULT));
		this.add("no_cobweb_slowdown", OriginsPowerTypes.NO_SLOWDOWN.get().configure(new NoSlowdownConfiguration(OriginsBlockTags.COBWEBS), hidden));
		this.add("conduit_power_on_land", OriginsPowerTypes.CONDUIT_POWER_ON_LAND.get().configure(NoConfiguration.INSTANCE, hidden));

		this.add("aerial_combatant", ApoliPowers.MODIFY_DAMAGE_DEALT.get().configure(new ModifyDamageDealtConfiguration(ModifierUtil.fromAttributeModifier(new AttributeModifier("Extra damage while fall flying", 1, AttributeModifier.Operation.MULTIPLY_BASE))), PowerData.builder().addCondition(ApoliEntityConditions.FALL_FLYING.get().configure(NoConfiguration.INSTANCE)).build()));
		this.add("air_from_potions", ApoliPowers.ACTION_ON_ITEM_USE.get().configure(new ActionOnItemUseConfiguration(Holder.direct(ApoliItemConditions.INGREDIENT.get().configure(FieldConfiguration.of(Ingredient.of(Items.POTION)))), Holder.direct(ApoliEntityActions.GAIN_AIR.get().configure(FieldConfiguration.of(60))), ApoliDefaultActions.ITEM_DEFAULT.getHolder().orElseThrow(RuntimeException::new), ActionOnItemUseConfiguration.TriggerType.FINISH, 0), hidden));
		this.add("aqua_affinity", ApoliPowers.MULTIPLE.get().configure(new MultipleConfiguration<>(makeAquaAffinity()), PowerData.DEFAULT));
		this.add("aquatic", ApoliPowers.ENTITY_GROUP.get().configure(FieldConfiguration.of(MobType.WATER), hidden));
		this.add("arcane_skin", ApoliPowers.MODEL_COLOR.get().configure(new ColorConfiguration(0.5F, 0.5F, 1.0F, 0.7F), PowerData.DEFAULT));


		this.add("burn_in_daylight", ApoliPowers.BURN.get().configure(new BurnConfiguration(20, 6), PowerData.builder().addCondition(ApoliEntityConditions.and(ApoliEntityConditions.EXPOSED_TO_SUN.get().configure(NoConfiguration.INSTANCE), ApoliEntityConditions.INVISIBLE.get().configure(NoConfiguration.INSTANCE, inverted))).build()));
		this.add("cat_vision", ApoliPowers.NIGHT_VISION.get().configure(FieldConfiguration.of(0.4F), PowerData.builder().addCondition(ApoliEntityConditions.SUBMERGED_IN.get().configure(new TagConfiguration<>(FluidTags.WATER), inverted)).build()));
		this.add("claustrophobia", ApoliPowers.STACKING_STATUS_EFFECT.get().configure(
				new StackingStatusEffectConfiguration(ListConfiguration.of(
						new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false, true),
						new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, true, false, true)),
						-20, 361, 10), PowerData.builder().addCondition(ApoliEntityConditions.BLOCK_COLLISION.get().configure(new BlockCollisionConfiguration(new Vec3(0, 1, 0), ApoliDefaultConditions.BLOCK_DEFAULT.getHolder().orElseThrow()))).build())
		);
	}
}
