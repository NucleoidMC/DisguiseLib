package xyz.nucleoid.disguiselib.casts;

/**
 * Internal methods for managing entity disguises.
 */
public interface DisguiseMethods {
    /**
     * Updates custom name and its visibility.
     * Also sets no-gravity to true in order
     * to prevent the client from predicting
     * the entity position and velocity.
     */
    void updateTrackedData();
}
