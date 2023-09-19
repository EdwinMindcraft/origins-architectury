package io.github.edwinmindcraft.origins.api.origin;

import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;

public interface IOriginCallbackPower<T extends IDynamicFeatureConfiguration> {
    @SuppressWarnings("unchecked")
    default <F extends PowerFactory<T>> void onChosen(ConfiguredPower<T, F> power, Entity living, boolean isOrb) {
       if (power.getFactory() instanceof IOriginCallbackPower) {
            ((IOriginCallbackPower<T>) power.getFactory()).onChosen(power.getConfiguration(), living, isOrb);
        }
    }

    @SuppressWarnings("unchecked")
    default <F extends PowerFactory<T>> void prepare(ConfiguredPower<T, F> power, Entity living, boolean isOrb) {
        if (power.getFactory() instanceof IOriginCallbackPower) {
            ((IOriginCallbackPower<T>) power.getFactory()).prepare(power.getConfiguration(), living, isOrb);
        }
    }

    @SuppressWarnings("unchecked")
    default <F extends PowerFactory<T>> boolean isReady(ConfiguredPower<T, F> power, Entity living, boolean isOrb) {
        if (power.getFactory() instanceof IOriginCallbackPower) {
            return ((IOriginCallbackPower<T>) power.getFactory()).isReady(power.getConfiguration(), living, isOrb);
        }
        return false;
    }

    void onChosen(T configuration, Entity entity, boolean isOrb);

    /**
     * Whether the power is considered ready upon being chosen.
     * @param configuration The configuration of the power.
     * @param entity The entity that chose the power.
     * @param isOrb If the power was chosen through an Orb of Origin.
     */
    default boolean isReady(T configuration, Entity entity, boolean isOrb) {
        return true;
    }

    /**
     * Prepares the power if it is not considered ready upon being chosen.
     * @param configuration The configuration of the power.
     * @param entity The entity that chose the power.
     * @param isOrb If the power was chosen through an Orb of Origin.
     */
    default void prepare(T configuration, Entity entity, boolean isOrb) {}


}
