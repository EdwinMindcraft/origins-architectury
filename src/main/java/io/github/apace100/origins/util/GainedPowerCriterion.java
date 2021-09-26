package io.github.apace100.origins.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.origins.Origins;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public class GainedPowerCriterion extends SimpleCriterionTrigger<GainedPowerCriterion.Conditions> {

    public static GainedPowerCriterion INSTANCE = new GainedPowerCriterion();

    private static final ResourceLocation ID = new ResourceLocation(Origins.MODID, "gained_power");

    @Override
    protected Conditions createInstance(JsonObject obj, EntityPredicate.Composite playerPredicate, DeserializationContext predicateDeserializer) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "power"));
        return new Conditions(playerPredicate, id);
    }

    public void trigger(ServerPlayer player, PowerType type) {
        this.trigger(player, (conditions -> conditions.matches(type)));
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public static class Conditions extends AbstractCriterionTriggerInstance {
        private final ResourceLocation powerId;

        public Conditions(EntityPredicate.Composite player, ResourceLocation powerId) {
            super(GainedPowerCriterion.ID, player);
            this.powerId = powerId;
        }

        public boolean matches(PowerType powerType) {
            return powerType.getIdentifier().equals(powerId);
        }

        public JsonObject serializeToJson(SerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("power", new JsonPrimitive(powerId.toString()));
            return jsonObject;
        }
    }
}
