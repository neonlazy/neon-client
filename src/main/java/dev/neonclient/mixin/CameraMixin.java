package dev.neonclient.mixin;

import dev.neonclient.esp.EspRenderer;
import dev.neonclient.module.FreecamModule;
import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void neon$freecam(Level level, Entity entity, boolean thirdPerson, boolean inverseView,
                              float partialTick, CallbackInfo ci) {
        FreecamModule fc = ModuleManager.FREECAM;
        if (!fc.isActive()) return;
        fc.frameUpdate();
        this.setPosition(fc.x, fc.y, fc.z);
        this.setRotation(fc.yaw, fc.pitch);
    }

    /** Posizione/rotazione reali della camera, usate dall'ESP per proiettare sullo schermo. */
    @Inject(method = "setPosition(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), require = 0)
    private void neon$capturePos(Vec3 pos, CallbackInfo ci) {
        EspRenderer.camX = pos.x;
        EspRenderer.camY = pos.y;
        EspRenderer.camZ = pos.z;
        EspRenderer.haveCam = true;
    }

    @Inject(method = "setRotation(FF)V", at = @At("HEAD"), require = 0)
    private void neon$captureRot(float yRot, float xRot, CallbackInfo ci) {
        EspRenderer.camYaw = yRot;
        EspRenderer.camPitch = xRot;
    }
}
