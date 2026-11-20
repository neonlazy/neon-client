package dev.neonclient.mixin;

import dev.neonclient.esp.EspRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disegna l'ESP come primo livello dell'HUD (sotto hotbar e chat). */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void neon$esp(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        EspRenderer.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
