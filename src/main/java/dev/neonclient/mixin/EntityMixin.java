package dev.neonclient.mixin;

import dev.neonclient.module.FreecamModule;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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

    /** Colore del contorno glowing dei giocatori (Player ESP). */
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void neon$espColor(CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.PLAYER_ESP.enabled && (Object) this instanceof Player
                && (Object) this != Minecraft.getInstance().player) {
            cir.setReturnValue(ModuleManager.PLAYER_ESP.glowColor());
        }
    }
}
