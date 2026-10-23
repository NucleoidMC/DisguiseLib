package xyz.nucleoid.disguiselib.impl.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(V1460.class)
public class V1460Mixin {
    @ModifyReturnValue(method = {"lambda$registerTypes$5"}, at = @At("RETURN"))
    private static TypeTemplate fixDisguises(TypeTemplate original, @Local(argsOnly = true, name = "schema") Schema schema) {
        return DSL.and(
                DSL.optional(DSL.field("DisguiseLib", DSL.optionalFields("DisguiseEntity", References.ENTITY_TREE.in(schema)))),
                original
        );
    }
}
