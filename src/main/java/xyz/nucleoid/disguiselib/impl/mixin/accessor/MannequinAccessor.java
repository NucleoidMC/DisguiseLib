package xyz.nucleoid.disguiselib.impl.mixin.accessor;

import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.entity.decoration.Mannequin.class)
public interface MannequinAccessor {
    @Invoker
    void callSetProfile(final ResolvableProfile profile);
}
