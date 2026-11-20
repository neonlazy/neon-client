package dev.neonclient.mixin;

import dev.neonclient.module.FreecamModule;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
