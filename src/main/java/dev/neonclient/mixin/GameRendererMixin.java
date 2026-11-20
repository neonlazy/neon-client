package dev.neonclient.mixin;

import dev.neonclient.module.FreecamModule;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Durante la freecam il mirino punta dalla camera libera: il blocco mirato diventa il bersaglio
 * del giocatore solo se e' entro la sua portata (come vuole il server), cosi' il corpo fermo
 * puo' rompere i blocchi intorno a se'.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void neon$freecamPick(float partialTick, CallbackInfo ci) {
        FreecamModule fc = ModuleManager.FREECAM;
        if (!fc.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        Vec3 from = new Vec3(fc.x, fc.y, fc.z);
        Vec3 dir = Vec3.directionFromRotation(fc.pitch, fc.yaw);
        double max = Math.min(96.0, from.distanceTo(p.getEyePosition()) + p.blockInteractionRange() + 1.0);
        BlockHitResult hit = mc.level.clip(new ClipContext(from, from.add(dir.scale(max)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));

        mc.crosshairPickEntity = null;
        if (hit.getType() == HitResult.Type.BLOCK && inReach(p, hit.getBlockPos())) {
            mc.hitResult = hit;
        } else {
            mc.hitResult = BlockHitResult.miss(hit.getLocation(), hit.getDirection(), hit.getBlockPos());
        }
    }

    /** Stesso controllo del server: il blocco deve essere entro la portata dagli occhi del giocatore. */
    private static boolean inReach(LocalPlayer p, BlockPos pos) {
        double r = p.blockInteractionRange();
        return new AABB(pos).distanceToSqr(p.getEyePosition()) < r * r;
    }
}
