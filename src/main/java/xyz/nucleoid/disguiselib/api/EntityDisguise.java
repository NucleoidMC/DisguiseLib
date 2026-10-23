package xyz.nucleoid.disguiselib.api;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Util for disguising entities.
 */
public interface EntityDisguise {

    /**
     * Tells you the disguised status.
     *
     * @return true if entity is disguised, otherwise false.
     */
    boolean disguiseLib$isDisguised();

    /**
     * Sets entity's disguise from {@link EntityType}
     *
     * @param entityType the type to disguise this entity into
     */
    void disguiseLib$disguiseAs(EntityType<?> entityType);

    /**
     * Sets entity's disguise from {@link EntityType}
     *
     * @param entity the entity to disguise into
     */
    void disguiseLib$disguiseAs(Entity entity);

    /**
     * Clears the disguise - sets the disguiseType back to original.
     */
    void disguiseLib$removeDisguise();

    /**
     * Gets the disguise entity type
     *
     * @return disguise entity type or real type if there's no disguise
     */
    EntityType<?> disguiseLib$getDisguiseType();

    /**
     * Gets the disguise entity.
     *
     * @return disguise entity or null if there's no disguise
     */
    Entity disguiseLib$getDisguiseEntity();

    /**
     * Whether this entity can bypass the
     * "disguises" and see entities normally
     * Intended more for admins (to not get trolled themselves).
     *
     * @return if entity can be "fooled" by disguise
     */
    boolean disguiseLib$hasTrueSight();

    /**
     * Toggles true sight - whether entity
     * can see disguises or not.
     * Intended more for admins (to not get trolled themselves).
     *
     * @param trueSight if entity should not see disguises
     */
    void disguiseLib$setTrueSight(boolean trueSight);

    /**
     * Gets the {@link GameProfile} for disguised entity,
     * used when disguising as player.
     *
     * @return GameProfile of the entity if set, otherwise null.
     */
    GameProfile disguiseLib$getGameProfile();

    /**
     * Sets the GameProfile.
     *
     * @param gameProfile a new profile for the entity.
     */
    void disguiseLib$setGameProfile(GameProfile gameProfile);
}
