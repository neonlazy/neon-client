package dev.neonclient.mixin;

import dev.neonclient.module.ModuleManager;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Xray: si disegnano solo le facce dei blocchi scelti, tutte (anche quelle a contatto con altri blocchi). */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void neon$xray(BlockState state, BlockState neighbor, Direction direction,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.XRAY.enabled) cir.setReturnValue(ModuleManager.XRAY.isVisible(state.getBlock()));
    }
}
