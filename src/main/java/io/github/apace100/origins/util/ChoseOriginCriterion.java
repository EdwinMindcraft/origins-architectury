package io.github.apace100.origins.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.origins.Origins;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

public class ChoseOriginCriterion extends SimpleCriterionTrigger<ChoseOriginCriterion.Conditions> {

	public static final ChoseOriginCriterion INSTANCE = new ChoseOriginCriterion();
	private static final ResourceLocation ID = Origins.identifier("chose_origin");

	public void trigger(ServerPlayer player, ResourceKey<Origin> origin) {
		this.trigger(player, (conditions -> conditions.matches(origin)));
	}

	@Override
	public Codec<Conditions> codec() {
		return Conditions.CODEC;
	}

	public record Conditions(Optional<ContextAwarePredicate> player, ResourceLocation originId) implements SimpleInstance {
		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChoseOriginCriterion.Conditions::player),
				ResourceLocation.CODEC.fieldOf("origin").forGetter(ChoseOriginCriterion.Conditions::originId)
		).apply(inst, Conditions::new));

		public boolean matches(ResourceKey<Origin> origin) {
			return Objects.equals(origin.location(), this.originId);
		}

		@Override
		public Optional<ContextAwarePredicate> player() {
			return player;
		}
	}
}
