package dev.neonclient.mixin;

import dev.neonclient.esp.EspGlow;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** ESP: le entita' scelte brillano (glowing solo lato client, visibile attraverso i muri). */
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void neon$espGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (EspGlow.shouldGlow(entity)) cir.setReturnValue(true);
    }

    /** Durante la freecam si possono rompere blocchi (entro la portata del corpo) ma non usare oggetti. */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void neon$noUse(CallbackInfo ci) {
        if (ModuleManager.FREECAM.isActive()) ci.cancel();
    }
}
