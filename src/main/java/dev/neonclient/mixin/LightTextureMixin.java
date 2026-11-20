package dev.neonclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Xray: luce piena (gamma altissima) cosi' i minerali dentro la roccia non sono neri. */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @WrapOperation(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object neon$fullbright(OptionInstance<?> option, Operation<Object> original) {
        Object value = original.call(option);
        if (ModuleManager.XRAY.enabled && option == Minecraft.getInstance().options.gamma()) {
            return 16.0;
        }
        return value;
    }
}
