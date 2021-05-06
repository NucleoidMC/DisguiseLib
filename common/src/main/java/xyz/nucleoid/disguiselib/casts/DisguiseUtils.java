package xyz.nucleoid.disguiselib.casts;

import net.minecraft.entity.LivingEntity;

/**
 * Internal methods for managing entity disguises.
 */
public interface DisguiseUtils {
    /**
     * Updates custom name and its visibility.
     * Also sets no-gravity to true in order
     * to prevent the client from predicting
     * the entity position and velocity.
     */
    void updateTrackedData();

    /**
     * Whether disguise type entity is an instance of {@link LivingEntity}.
     *
     * @return true whether the disguise type is an instance of {@link LivingEntity}, otherwise false.
     */
    boolean disguiseAlive();
}
