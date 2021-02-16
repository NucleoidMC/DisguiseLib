package org.samo_lego.disguiselib;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;

public interface EntityDisguise {

    /**
     * Tells you the disguised status.
     * @return true if entity is disguised, otherwise false.
     */
    boolean isDisguised();

    /**
     * Sets entity's disguise from {@link EntityType}
     * @param entityType the type to disguise this entity into
     */
    void disguiseAs(EntityType<?> entityType);

    /**
     * Clears the disguise - sets the disguiseType back to original.
     */
    void removeDisguise();

    /**
     * Gets the disguise entity type
     * @return disguise entity type or real type if there's no disguise
     */
    EntityType<?> getDisguiseType();

    /**
     * Whether disguise type entity is an instance of {@link LivingEntity}.
     * @return true whether the disguise type is an instance of {@link LivingEntity}, otherwise false.
     */
    boolean disguiseAlive();
}
