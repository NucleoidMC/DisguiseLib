package xyz.nucleoid.disguiselib.impl.mixin;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.server.network.ServerPlayNetworkHandler$1")
public class ServerPlayNetworkHandlerMixin_HitCheck {

    /*@Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;disconnect(Lnet/minecraft/text/Text;)V"
            ),
            cancellable = true
    )
    private void onEntityHit(CallbackInfo ci) {
        // Prevents disconnect
        // todo - apply only if disguised
        ci.cancel();
    }*/
}
