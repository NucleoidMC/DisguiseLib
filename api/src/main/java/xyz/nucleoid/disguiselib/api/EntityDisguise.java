package xyz.nucleoid.disguiselib.api;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public interface EntityDisguise {

    /**
     * Tells you the disguised status.
     *
     * @return true if entity is disguised, otherwise false.
     */
    boolean isDisguised();

    /**
     * Sets entity's disguise from {@link EntityType}
     *
     * @param entityType the type to disguise this entity into
     */
    void disguiseAs(EntityType<?> entityType);

    /**
     * Sets entity's disguise from {@link EntityType}
     *
     * @param entity the entity to disguise into
     */
    void disguiseAs(Entity entity);

    /**
     * Clears the disguise - sets the disguiseType back to original.
     */
    void removeDisguise();

    /**
     * Gets the disguise entity type
     *
     * @return disguise entity type or real type if there's no disguise
     */
    EntityType<?> getDisguiseType();

    /**
     * Gets the disguise entity.
     *
     * @return disguise entity or null if there's no disguise
     */
    @Nullable
    Entity getDisguiseEntity();

    /**
     * Whether this entity can bypass the
     * "disguises" and see entities normally
     * Intended more for admins (to not get trolled themselves).
     *
     * @return if entity can be "fooled" by disguise
     */
    boolean hasTrueSight();

    /**
     * Toggles true sight - whether entity
     * can see disguises or not.
     * Intended more for admins (to not get trolled themselves).
     *
     * @param trueSight if entity should not see disguises
     */
    void setTrueSight(boolean trueSight);

    /**
     * Gets the {@link GameProfile} for disguised entity,
     * used when disguising as player.
     *
     * @return GameProfile of the entity.
     */
    @Nullable GameProfile getGameProfile();

    /**
     * Sets the GameProfile
     *
     * @param gameProfile a new profile for the entity.
     */
    void setGameProfile(@Nullable GameProfile gameProfile);
}
