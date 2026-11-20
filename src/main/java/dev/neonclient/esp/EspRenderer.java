package dev.neonclient.esp;

import dev.neonclient.module.BlockEspModule;
import dev.neonclient.module.EntityEspModule;
import dev.neonclient.module.ItemEspModule;
import dev.neonclient.module.ModuleManager;
import dev.neonclient.module.PlayerEspModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * ESP disegnato come overlay 2D sopra il mondo: ogni oggetto viene proiettato sullo schermo,
 * quindi resta visibile attraverso qualsiasi muro e a qualsiasi distanza entro la render distance.
 */
public final class EspRenderer {
    public static volatile double camX, camY, camZ;
    public static volatile float camYaw, camPitch;
    public static volatile boolean haveCam;

    private static double ox, oy, oz;
    private static double fx, fy, fz, rx, rz, ux, uy, uz;
    private static double tanHalf, aspect;
    private static int sw, sh;
    private static int bx1, by1, bx2, by2;
    private static double sx, sy;

    private EspRenderer() {}

    public static void render(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        BlockEspModule be = ModuleManager.BLOCK_ESP;
        PlayerEspModule pe = ModuleManager.PLAYER_ESP;
        EntityEspModule ee = ModuleManager.ENTITY_ESP;
        ItemEspModule ie = ModuleManager.ITEM_ESP;
        boolean any = be.enabled || (pe.enabled && pe.tracers.value) || (ee.enabled && ee.tracers.value)
                || (ie.enabled && ie.tracers.value);

        if (any && prepare(g, mc, partialTick)) {
            if (be.enabled) drawBlocks(g, mc, be);
            if ((pe.enabled && pe.tracers.value) || (ee.enabled && ee.tracers.value) || (ie.enabled && ie.tracers.value)) {
                drawEntityTracers(g, mc, partialTick);
            }
        }
    }

    private static boolean prepare(GuiGraphics g, Minecraft mc, float pt) {
        sw = g.guiWidth();
        sh = g.guiHeight();
        if (sw <= 0 || sh <= 0) return false;

        double yaw, pitch;
        if (haveCam) {
            ox = camX; oy = camY; oz = camZ;
            yaw = camYaw; pitch = camPitch;
        } else {
            Vec3 eye = mc.player.getEyePosition(pt);
            ox = eye.x; oy = eye.y; oz = eye.z;
            yaw = mc.player.getViewYRot(pt);
            pitch = mc.player.getViewXRot(pt);
        }
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        fx = -Math.sin(yr) * Math.cos(pr);
        fy = -Math.sin(pr);
        fz = Math.cos(yr) * Math.cos(pr);
        rx = -Math.cos(yr);
        rz = -Math.sin(yr);
        // up = right x forward
        ux = -rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy;

        double fov = mc.options.fov().get();
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
        aspect = sw / (double) sh;
        return true;
    }

    private static boolean project(double wx, double wy, double wz) {
        double dx = wx - ox, dy = wy - oy, dz = wz - oz;
        double zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.05) return false;
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        sx = sw / 2.0 * (1.0 + xc / (zc * tanHalf * aspect));
        sy = sh / 2.0 * (1.0 - yc / (zc * tanHalf));
        return true;
    }

    private static boolean projectBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double wx = (i & 1) == 0 ? x1 : x2;
            double wy = (i & 2) == 0 ? y1 : y2;
            double wz = (i & 4) == 0 ? z1 : z2;
            if (!project(wx, wy, wz)) return false;
            if (sx < minX) minX = sx;
            if (sx > maxX) maxX = sx;
            if (sy < minY) minY = sy;
            if (sy > maxY) maxY = sy;
        }
        if (maxX < 0 || minX > sw || maxY < 0 || minY > sh) return false;
        bx1 = (int) Math.max(-2000, Math.min(4000, minX));
        by1 = (int) Math.max(-2000, Math.min(4000, minY));
        bx2 = (int) Math.max(-2000, Math.min(4000, maxX));
        by2 = (int) Math.max(-2000, Math.min(4000, maxY));
        if (bx2 - bx1 < 2) bx2 = bx1 + 2;
        if (by2 - by1 < 2) by2 = by1 + 2;
        return true;
    }

    private static void outline(GuiGraphics g, int rgb, int fillAlpha) {
        int c = 0xFF000000 | (rgb & 0xFFFFFF);
        if (fillAlpha > 0) g.fill(bx1 + 1, by1 + 1, bx2, by2, (fillAlpha << 24) | (rgb & 0xFFFFFF));
        g.fill(bx1, by1, bx2 + 1, by1 + 1, c);
        g.fill(bx1, by2, bx2 + 1, by2 + 1, c);
        g.fill(bx1, by1, bx1 + 1, by2 + 1, c);
        g.fill(bx2, by1, bx2 + 1, by2 + 1, c);
    }

    /**
     * Il glowing dei blocchi lo fanno le entita' di BlockGlow; qui un riquadro solo per i blocchi
     * senza modello (barrier, acqua, lava, light...) e le linee tracker.
     */
    private static void drawBlocks(GuiGraphics g, Minecraft mc, BlockEspModule be) {
        for (BlockPos p : be.snapshot()) {
            BlockState st = mc.level.getBlockState(p);
            int rgb = be.colorFor(st.getBlock());
            if (!BlockGlow.canGlow(st)
                    && projectBox(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0)) {
                outline(g, rgb, 0x20);
            }
            if (be.tracers.value) tracer(g, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, rgb);
        }
    }

    /** Linee tracker verso giocatori, mob e oggetti (ognuno col colore del suo modulo). */
    private static void drawEntityTracers(GuiGraphics g, Minecraft mc, float pt) {
        PlayerEspModule pe = ModuleManager.PLAYER_ESP;
        EntityEspModule ee = ModuleManager.ENTITY_ESP;
        ItemEspModule ie = ModuleManager.ITEM_ESP;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || BlockGlow.isOurs(e)) continue;
            int rgb;
            if (e instanceof Player) {
                if (!(pe.enabled && pe.tracers.value)) continue;
                rgb = pe.color;
            } else {
                int c = ee.enabled && ee.tracers.value ? ee.colorFor(e) : -1;
                if (c < 0 && ie.enabled && ie.tracers.value) c = ie.colorFor(e);
                if (c < 0) continue;
                rgb = c;
            }
            double x = Mth.lerp(pt, e.xo, e.getX());
            double y = Mth.lerp(pt, e.yo, e.getY()) + e.getBbHeight() / 2.0;
            double z = Mth.lerp(pt, e.zo, e.getZ());
            tracer(g, x, y, z, rgb);
        }
    }

    /** Linea dal mirino (centro dello schermo) al punto nel mondo. */
    private static void tracer(GuiGraphics g, double wx, double wy, double wz, int rgb) {
        if (!project(wx, wy, wz)) return;
        float cx = sw / 2f, cy = sh / 2f;
        float dx = (float) sx - cx, dy = (float) sy - cy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) return;
        len = Math.min(len, 6000f);
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate((float) Math.atan2(dy, dx));
        g.fill(0, 0, (int) len, 1, 0xC0000000 | (rgb & 0xFFFFFF));
        g.pose().popMatrix();
    }
}
