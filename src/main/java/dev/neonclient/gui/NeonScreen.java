package dev.neonclient.gui;

import dev.neonclient.NeonClient;
import dev.neonclient.NeonConfig;
import dev.neonclient.NeonConfig.Style;
import dev.neonclient.module.BlockEspModule;
import dev.neonclient.module.BoolSetting;
import dev.neonclient.module.Category;
import dev.neonclient.module.Module;
import dev.neonclient.module.ModuleManager;
import dev.neonclient.module.NumberSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

public class NeonScreen extends Screen {
    private static final String[] TABS = {"Modules", "Config", "Customize", "Macros", "Profiles"};
    private static final Category[] COLUMNS = {
            Category.MOVEMENT, Category.COMBAT, Category.WORLD, Category.PLAYER,
            Category.RENDER, Category.MISC, Category.FAVORITES};

    private static final int COL_W = 80;
    private static final int GAP = 5;
    private static final int ROW = 11;
    /** Scala della GUI (0.75 = 25% piu' piccola). */
    static final float SCALE = 0.75f;
    /** Centro orizzontale dei contenuti, leggermente a sinistra del centro schermo. */
    static final float ANCHOR = 0.42f;

    private Ui ui;
    private int tab = 0;
    private Module settingsModule;
    private boolean listening;
    private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];

    // costellazione
    private static final int N = 40;
    private float[] px, py, pvx, pvy;
    private int lastW, lastH;
    private long lastFrame;

    public NeonScreen() {
        super(Component.literal("Neon Client"));
        long win = GLFW.glfwGetCurrentContext();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            prevKeys[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
        }
    }

    @Override
    protected void init() {
        if (ui == null) ui = new Ui(this.font);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !listening;
    }

    @Override
    public void removed() {
        NeonConfig.save();
        super.removed();
    }

    /** Niente sfocatura vanilla: lo sfondo lo disegna la GUI stessa. */
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (ui == null) ui = new Ui(this.font);
        ui.beginFrame((int) (mouseX / SCALE), (int) (mouseY / SCALE));

        long win = GLFW.glfwGetCurrentContext();
        boolean[] down = new boolean[prevKeys.length];
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            down[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
        }

        if (listening && settingsModule != null) {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (down[k] && !prevKeys[k]) {
                    if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) settingsModule.key = 0;
                    else if (k != GLFW.GLFW_KEY_ESCAPE) settingsModule.key = k;
                    listening = false;
                    NeonConfig.save();
                    break;
                }
            }
        } else if (down[GLFW.GLFW_KEY_RIGHT_SHIFT] && !prevKeys[GLFW.GLFW_KEY_RIGHT_SHIFT]) {
            System.arraycopy(down, 0, prevKeys, 0, down.length);
            this.onClose();
            return;
        }
        System.arraycopy(down, 0, prevKeys, 0, down.length);

        int w = (int) Math.ceil(this.width / SCALE), h = (int) Math.ceil(this.height / SCALE);
        g.pose().pushMatrix();
        g.pose().scale(SCALE, SCALE);
        boolean popup = settingsModule != null;
        boolean lp = ui.lp, rp = ui.rp;
        if (popup) ui.consume(); // lo sfondo non riceve click mentre il popup e' aperto

        drawBackground(g, w, h);
        drawTabs(g, w);

        if (tab == 0) drawModules(g, w, h);
        else if (tab == 2) drawCustomize(g, w);
        else g.drawCenteredString(this.font, "Empty for now", (int) (w * ANCHOR), 50, Ui.TEXT_FAINT);
        drawWatermark(g, w, h);

        if (popup) {
            ui.lp = lp;
            ui.rp = rp;
            g.nextStratum();
            drawPopup(g, w, h);
        }
        g.pose().popMatrix();
    }

    /** X di partenza per un contenuto largo contentW, centrato sull'ancora (un po' a sinistra). */
    static int anchorX(int w, int contentW) {
        return Math.max(6, (int) (w * ANCHOR) - contentW / 2);
    }

    // ------------------------------------------------------------------ background

    private void drawBackground(GuiGraphics g, int w, int h) {
        Style style = NeonConfig.style();
        if (style == Style.GRADIENT) {
            g.fillGradient(0, 0, w, h, Ui.argb(NeonConfig.color1(), 0x50), Ui.argb(NeonConfig.color2(), 0xC0));
        } else if (style == Style.CONSTELLATION) {
            g.fill(0, 0, w, h, 0xC0050308);
            drawConstellation(g, w, h);
        } else {
            g.fill(0, 0, w, h, 0xA0050308);
        }
    }

    private void drawConstellation(GuiGraphics g, int w, int h) {
        if (px == null || w != lastW || h != lastH) {
            Random rnd = new Random();
            px = new float[N]; py = new float[N]; pvx = new float[N]; pvy = new float[N];
            for (int i = 0; i < N; i++) {
                px[i] = rnd.nextFloat() * w;
                py[i] = rnd.nextFloat() * h;
                pvx[i] = (rnd.nextFloat() - 0.5f) * 20f;
                pvy[i] = (rnd.nextFloat() - 0.5f) * 20f;
            }
            lastW = w;
            lastH = h;
            lastFrame = 0;
        }
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0f : Math.min((now - lastFrame) / 1_000_000_000f, 0.1f);
        lastFrame = now;

        int rgb = NeonConfig.color1();
        for (int i = 0; i < N; i++) {
            px[i] += pvx[i] * dt;
            py[i] += pvy[i] * dt;
            if (px[i] < 0 || px[i] > w) pvx[i] = -pvx[i];
            if (py[i] < 0 || py[i] > h) pvy[i] = -pvy[i];
            px[i] = Math.max(0, Math.min(w, px[i]));
            py[i] = Math.max(0, Math.min(h, py[i]));
        }
        final float maxD = 65f;
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                float dx = px[j] - px[i], dy = py[j] - py[i];
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d >= maxD) continue;
                int a = (int) (0x90 * (1f - d / maxD));
                int steps = Math.max(1, (int) (d / 3f));
                for (int s = 0; s <= steps; s++) {
                    int x = (int) (px[i] + dx * s / steps);
                    int y = (int) (py[i] + dy * s / steps);
                    g.fill(x, y, x + 1, y + 1, Ui.argb(rgb, a));
                }
            }
        }
        for (int i = 0; i < N; i++) {
            int x = (int) px[i], y = (int) py[i];
            g.fill(x - 1, y - 1, x + 2, y + 2, Ui.argb(rgb, 0xFF));
        }
    }

    // ------------------------------------------------------------------ top bar (invariata)

    private void drawTabs(GuiGraphics g, int w) {
        int total = 0;
        for (String t : TABS) total += this.font.width(t) + 14;
        int x = anchorX(w, total);
        g.fill(x, 2, x + total, 17, 0xE0161722);
        for (int i = 0; i < TABS.length; i++) {
            int tw = this.font.width(TABS[i]) + 14;
            boolean hover = ui.hover(x, 2, tw, 15);
            if (i == tab) g.fill(x, 2, x + tw, 17, Ui.argb(NeonConfig.color1(), 0xFF));
            else if (hover) g.fill(x, 2, x + tw, 17, 0x33FFFFFF);
            g.drawCenteredString(this.font, TABS[i], x + tw / 2, 6, Ui.WHITE);
            if (hover && ui.lp) tab = i;
            x += tw;
        }
    }

    // ------------------------------------------------------------------ modules

    private void drawModules(GuiGraphics g, int w, int h) {
        int perRow = Math.max(1, Math.min(COLUMNS.length, (w - 8 + GAP) / (COL_W + GAP)));
        int y = 26;
        for (int start = 0; start < COLUMNS.length; start += perRow) {
            int count = Math.min(perRow, COLUMNS.length - start);
            int rowW = count * COL_W + (count - 1) * GAP;
            int x = anchorX(w, rowW);
            int tallest = 0;
            for (int i = 0; i < count; i++) {
                int ph = drawCategory(g, COLUMNS[start + i], x + i * (COL_W + GAP), y);
                tallest = Math.max(tallest, ph);
            }
            y += tallest + 8;
        }
        g.drawString(this.font, "LMB toggle  |  RMB settings", 5, h - 12, Ui.TEXT_FAINT);
    }

    private int drawCategory(GuiGraphics g, Category c, int x, int y) {
        List<Module> mods = ModuleManager.byCategory(c);
        int bodyH = mods.isEmpty() ? 4 : mods.size() * ROW + 4;
        int ph = 12 + bodyH;
        ui.panel(g, x, y, COL_W, ph, c.label, String.valueOf(mods.size()));

        int ry = y + 14;
        for (Module m : mods) {
            boolean hov = ui.hover(x, ry, COL_W, ROW);
            if (m.enabled) {
                g.fill(x, ry, x + COL_W, ry + ROW, Ui.argb(Ui.accent(), 0x38));
                g.fill(x, ry, x + 2, ry + ROW, Ui.argb(Ui.accent(), 0xFF));
            } else if (hov) {
                g.fill(x, ry, x + COL_W, ry + ROW, 0x18FFFFFF);
            }
            String name = this.font.plainSubstrByWidth(m.name, COL_W - 14);
            g.drawString(this.font, name, x + 6, ry + 2, m.enabled ? Ui.WHITE : Ui.TEXT_DIM);
            if (m.key > 0) g.fill(x + COL_W - 5, ry + 4, x + COL_W - 3, ry + 6, Ui.argb(Ui.accent(), 0xFF));
            if (hov && ui.lp) m.toggle();
            if (hov && ui.rp) {
                settingsModule = m;
                listening = false;
            }
            ry += ROW;
        }
        return ph;
    }

    private void drawWatermark(GuiGraphics g, int w, int h) {
        String a = "NEON", b = " CLIENT", v = " v" + NeonClient.version();
        int tw = this.font.width(a + b + v);
        int x = w - tw - 5, y = h - 12;
        g.drawString(this.font, a, x, y, Ui.argb(Ui.accent(), 0xFF));
        x += this.font.width(a);
        g.drawString(this.font, b, x, y, Ui.WHITE);
        x += this.font.width(b);
        g.drawString(this.font, v, x, y, Ui.TEXT_FAINT);
    }

    // ------------------------------------------------------------------ module settings popup

    private int popupHeight(Module m) {
        int h = 12 + 6;
        h += m.settings.size() * 16;
        h += m.bools.size() * 18;
        if (m.hasColor) h += Ui.COLOR_EDITOR_H + 4;
        if (m instanceof BlockEspModule) h += 18;
        h += 18 + 14 + 6;
        return h;
    }

    private void drawPopup(GuiGraphics g, int w, int h) {
        Module m = settingsModule;
        int pw = 170;
        int ph = popupHeight(m);
        int x0 = anchorX(w, pw), y0 = Math.max(20, (h - ph) / 2);

        g.fill(0, 0, w, h, 0x70000000);
        ui.panel(g, x0, y0, pw, ph, m.name, "x");
        boolean closeHit = ui.hover(x0 + pw - 12, y0, 12, 12) && ui.lp;

        int cx = x0 + 6, cw = pw - 12;
        int cy = y0 + 18;
        for (NumberSetting s : m.settings) {
            s.set(ui.slider(g, "set:" + s.name, cx, cy, cw, s.name, s.min, s.max, s.value, s.step));
            cy += 16;
        }

        for (BoolSetting b : m.bools) {
            if (ui.button(g, cx, cy, cw, 14, b.name + ": " + (b.value ? "ON" : "OFF"), b.value)) {
                b.value = !b.value;
                NeonConfig.save();
            }
            cy += 18;
        }

        if (m.hasColor) {
            m.color = ui.colorEditor(g, "mod", cx, cy, cw, "Color", m.color);
            cy += Ui.COLOR_EDITOR_H + 4;
        }

        if (m instanceof BlockEspModule) {
            BlockEspModule be = (BlockEspModule) m;
            if (ui.button(g, cx, cy, cw, 14, "Select blocks (" + be.selected.size() + ")", false)) {
                ui.consume();
                NeonConfig.save();
                this.minecraft.setScreen(new BlockPickerScreen(this, be));
                return;
            }
            cy += 18;
        }

        String keyLabel = "Keybind: " + (listening ? "press a key..." : Ui.keyName(m.key));
        if (ui.button(g, cx, cy, cw, 14, keyLabel, listening)) listening = !listening;
        cy += 18;

        if (ui.button(g, cx, cy, cw, 14, m.favorite ? "Favorite  ★" : "Add to favorites  ☆", m.favorite)) {
            m.favorite = !m.favorite;
            NeonConfig.save();
        }

        if (closeHit || (ui.lp && !Ui.in(ui.mx, ui.my, x0, y0, pw, ph))) {
            settingsModule = null;
            listening = false;
            NeonConfig.save();
        }
    }

    // ------------------------------------------------------------------ customize

    private void drawCustomize(GuiGraphics g, int w) {
        boolean grad = NeonConfig.style() == Style.GRADIENT;
        int pw = 190;
        int ph = 12 + 6 + 10 + 18 + Ui.COLOR_EDITOR_H + 6 + (grad ? Ui.COLOR_EDITOR_H + 6 : 0);
        int x0 = anchorX(w, pw), y0 = 26;
        ui.panel(g, x0, y0, pw, ph, "Customize", "");

        int cx = x0 + 6, cw = pw - 12;
        int cy = y0 + 18;
        g.drawString(this.font, "Style", cx, cy + 1, Ui.TEXT_DIM);
        cy += 10;
        Style[] styles = Style.values();
        int bw = (cw - (styles.length - 1) * 3) / styles.length;
        for (int i = 0; i < styles.length; i++) {
            if (ui.button(g, cx + i * (bw + 3), cy, bw, 14, styles[i].label, NeonConfig.style() == styles[i])) {
                NeonConfig.setStyle(styles[i]);
            }
        }
        cy += 18;

        NeonConfig.setColor1(ui.colorEditor(g, "c1", cx, cy, cw, grad ? "Color 1" : "Color", NeonConfig.color1()));
        cy += Ui.COLOR_EDITOR_H + 6;
        if (grad) {
            NeonConfig.setColor2(ui.colorEditor(g, "c2", cx, cy, cw, "Color 2", NeonConfig.color2()));
        }
    }
}
