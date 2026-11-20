package dev.neonclient.gui;

import dev.neonclient.NeonConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** Stile grafico condiviso + widget "immediate mode" (input letto da GLFW a ogni frame). */
public final class Ui {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF9A93AB;
    public static final int TEXT_FAINT = 0xFF5E5870;
    public static final int PANEL_BG = 0xEE0B0912;
    public static final int HEADER_BG = 0xFF120E1C;
    public static final int WIDGET_BG = 0xFF1A1524;
    public static final int WIDGET_HOVER = 0xFF261E36;

    public static final int[] PRESETS = {
            0xFF3B3B, 0xFF8C1A, 0xFFD60A, 0x2BFF88, 0x00D4FF, 0x3A6BFF, 0x7B2CFF, 0xA020F0, 0xD946EF, 0xFF3CAC, 0xFFFFFF};

    // stato input del frame corrente
    public int mx, my;
    public boolean l, lp, rp;
    private boolean prevL, prevR;
    private String dragId;

    public final Font font;

    public Ui(Font font) {
        this.font = font;
        long win = GLFW.glfwGetCurrentContext();
        prevL = GLFW.glfwGetMouseButton(win, 0) == GLFW.GLFW_PRESS;
        prevR = GLFW.glfwGetMouseButton(win, 1) == GLFW.GLFW_PRESS;
    }

    public void beginFrame(int mouseX, int mouseY) {
        long win = GLFW.glfwGetCurrentContext();
        boolean left = GLFW.glfwGetMouseButton(win, 0) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(win, 1) == GLFW.GLFW_PRESS;
        mx = mouseX;
        my = mouseY;
        l = left;
        lp = left && !prevL;
        rp = right && !prevR;
        prevL = left;
        prevR = right;
        if (!left) dragId = null;
    }

    /** Consuma il click corrente (evita che lo stesso click venga usato due volte). */
    public void consume() {
        lp = false;
        rp = false;
    }

    // ------------------------------------------------------------------ colori

    public static int argb(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static int accent() {
        return NeonConfig.color1();
    }

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ testo (col carattere scelto)

    public void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font, NeonFont.of(s), x, y, color);
    }

    public void centered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawCenteredString(font, NeonFont.of(s), cx, y, color);
    }

    public int width(String s) {
        return font.width(NeonFont.of(s));
    }

    /** Taglia il testo perche' stia in maxW pixel. */
    public String trim(String s, int maxW) {
        if (width(s) <= maxW) return s;
        int end = s.length();
        while (end > 0 && width(s.substring(0, end)) > maxW) end--;
        return s.substring(0, end);
    }

    public boolean hover(int x, int y, int w, int h) {
        return in(mx, my, x, y, w, h);
    }

    // ------------------------------------------------------------------ elementi grafici

    /** Pannello: sfondo scuro, bordo sottile, testata con barra d'accento a sinistra. */
    public void panel(GuiGraphics g, int x, int y, int w, int h, String title, String right) {
        int acc = accent();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(acc, 0x50));
        panelBody(g, x, y, w, h);
        header(g, x, y, w, title, right);
    }

    /** Sfondo del pannello secondo lo stile scelto in Customize. */
    public static void panelBody(GuiGraphics g, int x, int y, int w, int h) {
        switch (NeonConfig.panelStyle()) {
            case GRADIENT -> g.fillGradient(x, y, x + w, y + h,
                    argb(NeonConfig.panelColor1(), 0xEE), argb(NeonConfig.panelColor2(), 0xEE));
            case GLASS -> g.fill(x, y, x + w, y + h, argb(NeonConfig.panelColor1(), 0x90));
            default -> g.fill(x, y, x + w, y + h, argb(NeonConfig.panelColor1(), 0xEE));
        }
    }

    public void header(GuiGraphics g, int x, int y, int w, String title, String right) {
        int acc = accent();
        if (NeonConfig.style() == NeonConfig.Style.GRADIENT) {
            g.fillGradient(x, y, x + w, y + 12, argb(acc, 0xFF), argb(NeonConfig.color2(), 0xFF));
        } else {
            g.fill(x, y, x + w, y + 12, HEADER_BG);
            g.fill(x, y, x + 2, y + 12, argb(acc, 0xFF));
            g.fill(x, y + 11, x + w, y + 12, argb(acc, 0x90));
        }
        text(g, title, x + 6, y + 2, WHITE);
        if (right != null && !right.isEmpty()) {
            text(g, right, x + w - 4 - width(right), y + 2,
                    NeonConfig.style() == NeonConfig.Style.GRADIENT ? WHITE : TEXT_FAINT);
        }
    }

    public boolean button(GuiGraphics g, int x, int y, int w, int h, String label, boolean active) {
        boolean hov = hover(x, y, w, h);
        int bg = active ? argb(accent(), 0xD0) : (hov ? WIDGET_HOVER : WIDGET_BG);
        g.fill(x, y, x + w, y + h, bg);
        if (!active && hov) g.fill(x, y + h - 1, x + w, y + h, argb(accent(), 0xA0));
        centered(g, label, x + w / 2, y + (h - 8) / 2, WHITE);
        return hov && lp;
    }

    /** Slider compatto su una sola riga (barra con testo sopra). */
    public double slider(GuiGraphics g, String id, int x, int y, int w, String label,
                         double min, double max, double value, double step) {
        int h = 12;
        if (lp && hover(x, y, w, h)) dragId = id;
        if (l && id.equals(dragId)) {
            double t = Math.max(0, Math.min(1, (mx - x) / (double) w));
            value = min + t * (max - min);
            if (step > 0) value = Math.round(value / step) * step;
        }
        value = Math.max(min, Math.min(max, value));
        double t = (value - min) / (max - min);
        g.fill(x, y, x + w, y + h, WIDGET_BG);
        g.fill(x, y, x + (int) (w * t), y + h, argb(accent(), 0xB0));
        String txt = step >= 1 ? String.valueOf((int) Math.round(value)) : String.format("%.1f", value);
        centered(g, label + ": " + txt, x + w / 2, y + 2, WHITE);
        return value;
    }

    /** Selettore colore: anteprima, colori rapidi e R/G/B. Altezza totale: COLOR_EDITOR_H. */
    public static final int COLOR_EDITOR_H = 10 + 16 + 3 * 15;

    public int colorEditor(GuiGraphics g, String id, int x, int y, int w, String label, int rgb) {
        text(g, label, x, y + 1, TEXT_DIM);
        g.fill(x + w - 22, y, x + w, y + 9, 0xFF000000);
        g.fill(x + w - 21, y + 1, x + w - 1, y + 8, argb(rgb, 0xFF));

        int py = y + 11;
        // i quadratini si stringono per starci tutti nella larghezza disponibile
        int n = PRESETS.length;
        int sz = Math.min(12, (w - (n - 1) * 2) / n);
        int step = n > 1 ? (w - sz) / (n - 1) : 0;
        step = Math.min(step, sz + 3);
        for (int i = 0; i < n; i++) {
            int sx = x + i * step;
            boolean sel = (rgb & 0xFFFFFF) == PRESETS[i];
            g.fill(sx - 1, py - 1, sx + sz + 1, py + 13, sel ? WHITE : 0xFF000000);
            g.fill(sx, py, sx + sz, py + 12, argb(PRESETS[i], 0xFF));
            if (lp && in(mx, my, sx, py, sz, 12)) rgb = PRESETS[i];
        }

        int r = (rgb >> 16) & 0xFF, gr = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int sy = y + 26;
        r = (int) Math.round(slider(g, id + "r", x, sy, w, "R", 0, 255, r, 1));
        gr = (int) Math.round(slider(g, id + "g", x, sy + 15, w, "G", 0, 255, gr, 1));
        b = (int) Math.round(slider(g, id + "b", x, sy + 30, w, "B", 0, 255, b, 1));
        return (r << 16) | (gr << 8) | b;
    }

    public static String keyName(int code) {
        if (code <= 0) return "None";
        switch (code) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "L-Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R-Alt";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            default: break;
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(code, 0);
        return n != null ? n.toUpperCase() : "Key " + code;
    }
}
