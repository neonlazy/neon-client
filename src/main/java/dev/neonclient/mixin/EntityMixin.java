package dev.neonclient.mixin;

import dev.neonclient.esp.EspGlow;
import dev.neonclient.module.FreecamModule;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Il mouse ruota la camera libera invece del giocatore. */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void neon$freecamLook(double yRot, double xRot, CallbackInfo ci) {
        FreecamModule fc = ModuleManager.FREECAM;
        if (fc.isActive() && (Object) this == Minecraft.getInstance().player) {
            fc.rotate(yRot, xRot);
            ci.cancel();
        }
    }

    /** Colore del contorno glowing degli ESP. */
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void neon$espColor(CallbackInfoReturnable<Integer> cir) {
        int c = EspGlow.colorFor((Entity) (Object) this);
        if (c >= 0) cir.setReturnValue(c);
    }
}
