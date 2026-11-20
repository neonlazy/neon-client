package dev.neonclient.gui;

import dev.neonclient.NeonClient;
import dev.neonclient.NeonConfig;
import dev.neonclient.NeonConfig.Style;
import dev.neonclient.module.BoolSetting;
import dev.neonclient.module.Catalog;
import dev.neonclient.module.Category;
import dev.neonclient.module.Module;
import dev.neonclient.module.ModuleManager;
import dev.neonclient.module.NumberSetting;
import dev.neonclient.module.SelectionModule;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Random;

public class NeonScreen extends Screen {
    private static final String[] TABS = {"Modules", "Config", "Customize"};
    private static final int TAB_MODULES = 0;
    private static final int TAB_CUSTOMIZE = 2;
    /** Colonne in ordine alfabetico, poi Favorites e Search (null). */
    private static final Category[] COLUMNS = {
            Category.COMBAT, Category.MISC, Category.MOVEMENT, Category.PLAYER,
            Category.RENDER, Category.WORLD, Category.FAVORITES, null};
    private static final int LIST_ROW = 14;
    private static final int LIST_MAX_ROWS = 8;

    private static final int COL_W = 80;
    private static final int GAP = 5;
    private static final int ROW = 11;
    /** Scala della GUI (0.75 = 25% piu' piccola). */
    static final float SCALE = 0.5625f;
    /** Centro orizzontale dei contenuti, leggermente a sinistra del centro schermo. */
    static final float ANCHOR = 0.34f;

    private Ui ui;
    private int tab = TAB_MODULES;
    private EditBox searchBox;
    private String searchQuery = "";
    private int sbX, sbY, sbW;

    /** Elemento di cui si sta cambiando il colore ("" = colore di default del modulo). */
    private String colorTarget;
    private double listScroll;
    private int listX, listY, listW, listH;
    private final java.util.Map<String, String> names = new java.util.HashMap<>();
    private final java.util.Map<String, ItemStack> icons = new java.util.HashMap<>();
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
        searchBox = new EditBox(this.font, 0, 0, COL_W - 8, 10, Component.literal("Search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(32);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(v -> searchQuery = v);
        searchBox.visible = false;
        this.addRenderableWidget(searchBox);
    }

    private boolean typing() {
        return searchBox != null && this.getFocused() == searchBox;
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

    /** Rotellina: scorre la lista degli elementi scelti nel popup (nessun @Override di proposito). */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        listScroll -= scrollY * LIST_ROW;
        return true;
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
        } else if (!typing() && down[GLFW.GLFW_KEY_RIGHT_SHIFT] && !prevKeys[GLFW.GLFW_KEY_RIGHT_SHIFT]) {
            System.arraycopy(down, 0, prevKeys, 0, down.length);
            this.onClose();
            return;
        }
        System.arraycopy(down, 0, prevKeys, 0, down.length);

        int w = (int) Math.ceil(this.width / SCALE), h = (int) Math.ceil(this.height / SCALE);
        g.pose().pushMatrix();
        g.pose().scale(SCALE, SCALE);
        if (settingsModule == null) colorTarget = null;
        boolean popup = settingsModule != null;
        boolean lp = ui.lp, rp = ui.rp;
        if (popup) ui.consume(); // lo sfondo non riceve click mentre il popup e' aperto

        // la barra di ricerca si attiva cliccandoci sopra, si disattiva cliccando altrove
        if (ui.lp && searchBox != null) {
            boolean onBox = searchBox.visible && ui.hover(sbX, sbY, sbW, 12);
            this.setFocused(onBox ? searchBox : null);
        }
        if (searchBox != null) searchBox.visible = false;

        drawBackground(g, w, h);
        drawTabs(g, w);

        if (tab == TAB_MODULES) drawModules(g, w, h);
        else if (tab == TAB_CUSTOMIZE) drawCustomize(g, w);
        else ui.centered(g, "Empty for now", w / 2, 50, Ui.TEXT_FAINT);
        drawWatermark(g, w, h);
        super.render(g, ui.mx, ui.my, partialTick);

        if (popup) {
            ui.lp = lp;
            ui.rp = rp;
            boolean editing = colorTarget != null;
            if (editing) ui.consume(); // il popup sotto non riceve click mentre si modifica un colore
            g.nextStratum();
            drawPopup(g, w, h);
            if (editing && settingsModule != null) {
                ui.lp = lp;
                ui.rp = rp;
                g.nextStratum();
                drawColorPopup(g, w, h);
            }
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
        for (String t : TABS) total += ui.width(t) + 14;
        int x = (w - total) / 2;
        g.fill(x, 2, x + total, 17, 0xE0161722);
        for (int i = 0; i < TABS.length; i++) {
            int tw = ui.width(TABS[i]) + 14;
            boolean hover = ui.hover(x, 2, tw, 15);
            if (i == tab) g.fill(x, 2, x + tw, 17, Ui.argb(NeonConfig.color1(), 0xFF));
            else if (hover) g.fill(x, 2, x + tw, 17, 0x33FFFFFF);
            ui.centered(g, TABS[i], x + tw / 2, 6, Ui.WHITE);
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
        ui.text(g, "LMB toggle  |  RMB settings", 5, h - 12, Ui.TEXT_FAINT);
    }

    private int drawCategory(GuiGraphics g, Category c, int x, int y) {
        boolean search = c == null;
        List<Module> mods = search ? ModuleManager.search(searchQuery) : ModuleManager.byCategory(c);
        int top = search ? 16 : 0; // spazio per la barra di ricerca
        int bodyH = top + (mods.isEmpty() ? 4 : mods.size() * ROW + 4);
        int ph = 12 + bodyH;
        ui.panel(g, x, y, COL_W, ph, search ? "Search" : c.label, search ? "" : String.valueOf(mods.size()));

        if (search && searchBox != null) {
            sbX = x + 3;
            sbY = y + 15;
            sbW = COL_W - 6;
            boolean focused = typing();
            g.fill(sbX, sbY, sbX + sbW, sbY + 12, Ui.WIDGET_BG);
            g.fill(sbX, sbY + 11, sbX + sbW, sbY + 12, Ui.argb(Ui.accent(), focused ? 0xFF : 0x60));
            searchBox.setX(sbX + 3);
            searchBox.setY(sbY + 2);
            searchBox.setWidth(sbW - 6);
            searchBox.visible = settingsModule == null;
        }

        int ry = y + 14 + top;
        for (Module m : mods) {
            boolean hov = ui.hover(x, ry, COL_W, ROW);
            if (m.enabled) {
                g.fill(x, ry, x + COL_W, ry + ROW, Ui.argb(Ui.accent(), 0x38));
                g.fill(x, ry, x + 2, ry + ROW, Ui.argb(Ui.accent(), 0xFF));
            } else if (hov) {
                g.fill(x, ry, x + COL_W, ry + ROW, 0x18FFFFFF);
            }
            String name = ui.trim(m.name, COL_W - 14);
            ui.text(g, name, x + 6, ry + 2, m.enabled ? Ui.WHITE : Ui.TEXT_DIM);
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
        int tw = ui.width(a + b + v);
        int x = w - tw - 5, y = h - 12;
        ui.text(g, a, x, y, Ui.argb(Ui.accent(), 0xFF));
        x += ui.width(a);
        ui.text(g, b, x, y, Ui.WHITE);
        x += ui.width(b);
        ui.text(g, v, x, y, Ui.TEXT_FAINT);
    }

    // ------------------------------------------------------------------ module settings popup

    private int popupHeight(Module m) {
        int h = 12 + 6;
        h += m.settings.size() * 16;
        h += m.bools.size() * 18;
        if (m instanceof SelectionModule sm) {
            h += 18; // pulsante selettore
            h += 18 + 14 + 6; // keybind + preferiti
            h += 16; // colore di default
            int n = sm.selected.size();
            h += 12 + (n == 0 ? 12 : Math.min(LIST_MAX_ROWS, n) * LIST_ROW) + 2;
        } else {
            if (m.hasColor) h += Ui.COLOR_EDITOR_H + 4;
            h += 18 + 14 + 6;
        }
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

        if (m.hasColor && !(m instanceof SelectionModule)) {
            m.color = ui.colorEditor(g, "mod", cx, cy, cw, "Color", m.color);
            cy += Ui.COLOR_EDITOR_H + 4;
        }

        if (m instanceof SelectionModule) {
            SelectionModule sm = (SelectionModule) m;
            String count = sm.selected.isEmpty() && sm.emptyMeansAll() ? "all" : String.valueOf(sm.selected.size());
            if (ui.button(g, cx, cy, cw, 14, "Select " + sm.noun() + " (" + count + ")", false)) {
                ui.consume();
                NeonConfig.save();
                this.minecraft.setScreen(new PickerScreen(this, sm));
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
        cy += 18;

        if (m instanceof SelectionModule sm) drawSelectedList(g, sm, cx, cy, cw);

        if (closeHit || (ui.lp && !Ui.in(ui.mx, ui.my, x0, y0, pw, ph))) {
            settingsModule = null;
            colorTarget = null;
            listening = false;
            NeonConfig.save();
        }
    }

    /** Colore di default + lista degli elementi scelti; tasto destro su una riga = cambia il suo colore. */
    private void drawSelectedList(GuiGraphics g, SelectionModule sm, int cx, int cy, int cw) {
        // colore di default (per gli elementi senza colore proprio e, se non c'e' selezione, per tutti)
        boolean hovDef = ui.hover(cx, cy, cw, 12);
        g.fill(cx, cy, cx + cw, cy + 12, hovDef ? Ui.WIDGET_HOVER : Ui.WIDGET_BG);
        ui.text(g, "Default color", cx + 4, cy + 2, Ui.WHITE);
        swatch(g, cx + cw - 18, cy + 2, sm.color);
        if (hovDef && (ui.rp || ui.lp)) colorTarget = "";
        cy += 16;

        List<String> ids = sm.selectedSorted();
        ui.text(g, "Selected " + sm.noun() + " (" + ids.size() + ")  -  RMB: color", cx, cy, Ui.TEXT_DIM);
        cy += 12;
        if (ids.isEmpty()) {
            ui.text(g, sm.emptyMeansAll() ? "None: all " + sm.noun() + " use the default color" : "None selected",
                    cx + 2, cy + 2, Ui.TEXT_FAINT);
            return;
        }

        int rows = Math.min(LIST_MAX_ROWS, ids.size());
        listX = cx;
        listY = cy;
        listW = cw;
        listH = rows * LIST_ROW;
        double maxScroll = Math.max(0, ids.size() * LIST_ROW - listH);
        listScroll = Math.max(0, Math.min(maxScroll, listScroll));

        g.fill(cx, cy, cx + cw, cy + listH, 0x50000000);
        g.enableScissor(cx, cy, cx + cw, cy + listH);
        boolean inList = ui.hover(cx, cy, cw, listH);
        for (int i = 0; i < ids.size(); i++) {
            int ry = cy + i * LIST_ROW - (int) listScroll;
            if (ry + LIST_ROW < cy || ry > cy + listH) continue;
            String id = ids.get(i);
            boolean hov = inList && ui.hover(cx, ry, cw, LIST_ROW);
            if (hov) g.fill(cx, ry, cx + cw, ry + LIST_ROW, 0x18FFFFFF);
            Catalog.Entry e = sm.entry(id);
            ItemStack icon = e == null ? ItemStack.EMPTY : icons.computeIfAbsent(id, k -> safeIcon(e));
            g.pose().pushMatrix();
            g.pose().translate(cx + 2, ry + 1);
            g.pose().scale(0.75f, 0.75f);
            g.renderItem(icon, 0, 0);
            g.pose().popMatrix();
            String name = e == null ? id : names.computeIfAbsent(id, k -> safeName(e));
            ui.text(g, ui.trim(name, cw - 40), cx + 17, ry + 3, Ui.TEXT_DIM);
            swatch(g, cx + cw - 18, ry + 3, sm.colorOf(id));
            if (hov && ui.rp) colorTarget = id;
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int thumbH = Math.max(8, (int) (listH * (listH / (double) (ids.size() * LIST_ROW))));
            int thumbY = cy + (int) ((listH - thumbH) * (listScroll / maxScroll));
            g.fill(cx + cw - 2, thumbY, cx + cw, thumbY + thumbH, Ui.argb(Ui.accent(), 0xFF));
        }
    }

    private static void swatch(GuiGraphics g, int x, int y, int rgb) {
        g.fill(x - 1, y - 1, x + 15, y + 9, 0xFF000000);
        g.fill(x, y, x + 14, y + 8, Ui.argb(rgb, 0xFF));
    }

    private static String safeName(Catalog.Entry e) {
        try {
            return e.name().get();
        } catch (Exception ex) {
            return e.shortName();
        }
    }

    private static ItemStack safeIcon(Catalog.Entry e) {
        try {
            return e.icon().get();
        } catch (Exception ex) {
            return ItemStack.EMPTY;
        }
    }

    /** Finestrella per cambiare il colore di un elemento (o quello di default). */
    private void drawColorPopup(GuiGraphics g, int w, int h) {
        if (!(settingsModule instanceof SelectionModule sm)) {
            colorTarget = null;
            return;
        }
        boolean def = colorTarget.isEmpty();
        Catalog.Entry e = def ? null : sm.entry(colorTarget);
        String title = def ? "Default color" : (e == null ? colorTarget : names.computeIfAbsent(colorTarget, k -> safeName(e)));
        int pw = 150, ph = 12 + 6 + Ui.COLOR_EDITOR_H + 6 + 14 + 6;
        int x0 = anchorX(w, pw) + 30, y0 = Math.max(20, (h - ph) / 2);
        ui.panel(g, x0, y0, pw, ph, ui.trim(title, pw - 20), "");

        int cx = x0 + 6, cw = pw - 12, cy = y0 + 18;
        int cur = def ? sm.color : sm.colorOf(colorTarget);
        int next = ui.colorEditor(g, "entry", cx, cy, cw, "Color", cur);
        if (next != cur) {
            if (def) {
                sm.color = next;
            } else {
                sm.setEntryColor(colorTarget, next);
            }
        }
        cy += Ui.COLOR_EDITOR_H + 6;

        int bw = (cw - 4) / 2;
        boolean canReset = !def && sm.hasCustomColor(colorTarget);
        if (ui.button(g, cx, cy, bw, 14, canReset ? "Reset" : "-", false) && canReset) {
            sm.resetEntryColor(colorTarget);
        }
        boolean done = ui.button(g, cx + bw + 4, cy, bw, 14, "Done", false);
        if (done || (ui.lp && !Ui.in(ui.mx, ui.my, x0, y0, pw, ph))) {
            colorTarget = null;
            NeonConfig.save();
        }
    }

    // ------------------------------------------------------------------ customize

    private void drawCustomize(GuiGraphics g, int w) {
        int pw = 170, gap = 8;
        int x0 = anchorX(w, pw * 2 + gap), y0 = 26;

        // --- sfondo della GUI
        boolean grad = NeonConfig.style() == Style.GRADIENT;
        int ph = 12 + 6 + 28 + 10 + 18 + Ui.COLOR_EDITOR_H + 6 + (grad ? Ui.COLOR_EDITOR_H + 6 : 0);
        ui.panel(g, x0, y0, pw, ph, "Background", "");
        int cx = x0 + 6, cw = pw - 12, cy = y0 + 18;

        ui.text(g, "Font", cx, cy + 1, Ui.TEXT_DIM);
        cy += 10;
        NeonConfig.FontChoice[] fonts = NeonConfig.FontChoice.values();
        int fw = (cw - (fonts.length - 1) * 3) / fonts.length;
        for (int i = 0; i < fonts.length; i++) {
            if (ui.button(g, cx + i * (fw + 3), cy, fw, 14, fonts[i].label, NeonConfig.font() == fonts[i])) {
                NeonConfig.setFont(fonts[i]);
            }
        }
        cy += 18;
        ui.text(g, "Style", cx, cy + 1, Ui.TEXT_DIM);
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
        if (grad) NeonConfig.setColor2(ui.colorEditor(g, "c2", cx, cy, cw, "Color 2", NeonConfig.color2()));

        // --- pannelli dei moduli (la parte scura)
        int x1 = x0 + pw + gap;
        boolean pgrad = NeonConfig.panelStyle() == NeonConfig.PanelStyle.GRADIENT;
        int ph2 = 12 + 6 + 10 + 18 + Ui.COLOR_EDITOR_H + 6 + (pgrad ? Ui.COLOR_EDITOR_H + 6 : 0);
        ui.panel(g, x1, y0, pw, ph2, "Modules", "");
        cx = x1 + 6;
        cy = y0 + 18;
        ui.text(g, "Panel style", cx, cy + 1, Ui.TEXT_DIM);
        cy += 10;
        NeonConfig.PanelStyle[] ps = NeonConfig.PanelStyle.values();
        bw = (cw - (ps.length - 1) * 3) / ps.length;
        for (int i = 0; i < ps.length; i++) {
            if (ui.button(g, cx + i * (bw + 3), cy, bw, 14, ps[i].label, NeonConfig.panelStyle() == ps[i])) {
                NeonConfig.setPanelStyle(ps[i]);
            }
        }
        cy += 18;
        NeonConfig.setPanelColor1(ui.colorEditor(g, "p1", cx, cy, cw, pgrad ? "Panel color 1" : "Panel color",
                NeonConfig.panelColor1()));
        cy += Ui.COLOR_EDITOR_H + 6;
        if (pgrad) NeonConfig.setPanelColor2(ui.colorEditor(g, "p2", cx, cy, cw, "Panel color 2", NeonConfig.panelColor2()));
    }
}
