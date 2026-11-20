package dev.neonclient.mixin;

import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Durante la freecam il corpo non deve rompere/piazzare/attaccare. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void neon$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.FREECAM.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void neon$noBreak(boolean leftClick, CallbackInfo ci) {
        if (ModuleManager.FREECAM.isActive()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void neon$noUse(CallbackInfo ci) {
        if (ModuleManager.FREECAM.isActive()) ci.cancel();
    }
}
