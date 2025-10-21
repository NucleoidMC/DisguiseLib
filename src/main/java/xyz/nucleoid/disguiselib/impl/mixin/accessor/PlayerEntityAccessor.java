package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerLikeEntity.class)
public interface PlayerEntityAccessor {
    @Accessor("PLAYER_MODE_CUSTOMIZATION_ID")
    static TrackedData<Byte> getPLAYER_MODEL_PARTS() {
        throw new AssertionError();
    }
}
