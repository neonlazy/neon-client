package dev.neonclient.esp;

import dev.neonclient.module.BlockEspModule;
import dev.neonclient.module.ModuleManager;
import dev.neonclient.module.PlayerEspModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
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
        PlayerEspModule pe = ModuleManager.PLAYER_ESP;
        BlockEspModule be = ModuleManager.BLOCK_ESP;
        if (!pe.enabled && !be.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        if (!prepare(g, mc, partialTick)) return;

        if (be.enabled) drawBlocks(g, be);
        if (pe.enabled) drawPlayers(g, mc, pe, partialTick);
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

    private static void drawBlocks(GuiGraphics g, BlockEspModule be) {
        int rgb = be.color;
        for (BlockPos p : be.snapshot()) {
            if (!projectBox(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0)) continue;
            outline(g, rgb, 0x20);
        }
    }

    private static void drawPlayers(GuiGraphics g, Minecraft mc, PlayerEspModule pe, float pt) {
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            double x = Mth.lerp(pt, p.xo, p.getX());
            double y = Mth.lerp(pt, p.yo, p.getY());
            double z = Mth.lerp(pt, p.zo, p.getZ());
            double hw = p.getBbWidth() / 2.0;
            double hh = p.getBbHeight();
            if (!projectBox(x - hw, y, z - hw, x + hw, y + hh, z + hw)) continue;
            outline(g, pe.color, 0x28);

            double dx = x - ox, dy = y - oy, dz = z - oz;
            String label = p.getName().getString() + "  " + (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + "m";
            int tw = mc.font.width(label);
            int tx = (bx1 + bx2) / 2 - tw / 2;
            int ty = by1 - 11;
            g.fill(tx - 3, ty - 2, tx + tw + 3, ty + 9, 0xB0000000);
            g.drawString(mc.font, label, tx, ty, 0xFF000000 | (pe.color & 0xFFFFFF));
        }
    }
}
