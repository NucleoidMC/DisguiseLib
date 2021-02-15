package org.samo_lego.disguiselib;

import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public interface EntityDisguise {
    boolean isDisguised();

    void disguiseAs(Identifier entityId);
    void disguiseAs(EntityType<?> entityType, boolean entityTypeAlive);
    void removeDisguise();

    EntityType<?> getDisguiseType();
    boolean disguiseAlive();
}
