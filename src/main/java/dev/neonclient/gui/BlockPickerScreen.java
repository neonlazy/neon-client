package dev.neonclient.gui;

import dev.neonclient.NeonConfig;
import dev.neonclient.module.BlockCatalog;
import dev.neonclient.module.BlockEspModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Selettore blocchi per Block ESP: ricerca per nome (italiano/inglese o id), tutti i blocchi del gioco. */
public class BlockPickerScreen extends Screen {
    private static final int ROW = 18;

    private final Screen parent;
    private final BlockEspModule module;
    private Ui ui;
    private EditBox search;
    private String query = "";
    private boolean onlySelected;
    private double scroll;
    private boolean draggingBar;

    private final Map<String, String> names = new HashMap<>();
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final boolean[] prevKeys = new boolean[GLFW.GLFW_KEY_LAST + 1];

    // layout
    private int px, py, pw, ph, listY, listH;

    public BlockPickerScreen(Screen parent, BlockEspModule module) {
        super(Component.literal("Block ESP"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        if (ui == null) ui = new Ui(this.font);
        pw = Math.min(300, this.width - 20);
        ph = this.height - 30;
        px = (this.width - pw) / 2;
        py = 15;

        search = new EditBox(this.font, px + 6, py + 17, pw - 12, 14, Component.literal("Search"));
        search.setHint(Component.literal("Search block..."));
        search.setMaxLength(64);
        search.setValue(query);
        search.setResponder(s -> {
            query = s;
            scroll = 0;
        });
        this.addRenderableWidget(search);
        this.setInitialFocus(search);

        listY = py + 54;
        listH = ph - 54 - 22;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        NeonConfig.save();
        this.minecraft.setScreen(parent);
    }

    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    /** Rotellina del mouse (nessun @Override: se la firma cambia, compila lo stesso). */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= scrollY * ROW * 2;
        return true;
    }

    private String nameOf(BlockCatalog.Entry e) {
        return names.computeIfAbsent(e.id(), k -> e.block().getName().getString());
    }

    private ItemStack iconOf(BlockCatalog.Entry e) {
        return icons.computeIfAbsent(e.id(), k -> new ItemStack(e.block()));
    }

    private List<BlockCatalog.Entry> visible() {
        String q = query.toLowerCase().trim();
        String qId = q.replace(' ', '_');
        List<BlockCatalog.Entry> out = new ArrayList<>();
        for (BlockCatalog.Entry e : BlockCatalog.all()) {
            if (onlySelected && !module.isSelected(e.id())) continue;
            if (!q.isEmpty() && !e.id().contains(qId) && !nameOf(e).toLowerCase().contains(q)) continue;
            out.add(e);
        }
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (ui == null) ui = new Ui(this.font);
        ui.beginFrame(mouseX, mouseY);
        handleKeys();

        g.fill(0, 0, this.width, this.height, 0xA0050308);
        ui.panel(g, px, py, pw, ph, "Block ESP  -  select blocks", module.selected.size() + " selected");

        // filtri
        int fy = py + 35;
        int bw = (pw - 12 - 6) / 3;
        if (ui.button(g, px + 6, fy, bw, 14, "All", !onlySelected)) { onlySelected = false; scroll = 0; }
        if (ui.button(g, px + 6 + bw + 3, fy, bw, 14, "Selected", onlySelected)) { onlySelected = true; scroll = 0; }
        if (ui.button(g, px + 6 + 2 * (bw + 3), fy, bw, 14, "Clear all", false)) module.clearSelection();

        // lista
        List<BlockCatalog.Entry> list = visible();
        int contentH = list.size() * ROW;
        double maxScroll = Math.max(0, contentH - listH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        int lx = px + 6, lw = pw - 12 - 6;
        g.fill(lx, listY, lx + lw, listY + listH, 0x60000000);
        g.enableScissor(lx, listY, lx + lw, listY + listH);
        int first = (int) (scroll / ROW);
        int last = Math.min(list.size(), first + listH / ROW + 2);
        boolean inList = Ui.in(ui.mx, ui.my, lx, listY, lw, listH);
        for (int i = first; i < last; i++) {
            BlockCatalog.Entry e = list.get(i);
            int ry = listY + i * ROW - (int) scroll;
            boolean sel = module.isSelected(e.id());
            boolean hov = inList && Ui.in(ui.mx, ui.my, lx, ry, lw, ROW);
            if (sel) {
                g.fill(lx, ry, lx + lw, ry + ROW, Ui.argb(Ui.accent(), 0x40));
                g.fill(lx, ry, lx + 2, ry + ROW, Ui.argb(Ui.accent(), 0xFF));
            } else if (hov) {
                g.fill(lx, ry, lx + lw, ry + ROW, 0x18FFFFFF);
            }
            g.renderItem(iconOf(e), lx + 4, ry + 1);
            String name = this.font.plainSubstrByWidth(nameOf(e), lw / 2 - 10);
            g.drawString(this.font, name, lx + 24, ry + 5, sel ? Ui.WHITE : Ui.TEXT_DIM);
            String id = this.font.plainSubstrByWidth(e.shortName(), lw / 2 - 20);
            g.drawString(this.font, id, lx + lw - 6 - this.font.width(id) - 12, ry + 5, Ui.TEXT_FAINT);
            g.drawString(this.font, sel ? "✔" : "+", lx + lw - 12, ry + 5,
                    sel ? Ui.argb(Ui.accent(), 0xFF) : Ui.TEXT_FAINT);
            if (hov && ui.lp) module.toggleBlock(e.id());
        }
        g.disableScissor();
        if (list.isEmpty()) {
            g.drawCenteredString(this.font, "No blocks found", lx + lw / 2, listY + 10, Ui.TEXT_FAINT);
        }

        // barra di scorrimento
        int sbx = lx + lw + 2;
        g.fill(sbx, listY, sbx + 4, listY + listH, Ui.WIDGET_BG);
        if (maxScroll > 0) {
            int thumbH = Math.max(12, (int) (listH * (listH / (double) contentH)));
            int thumbY = listY + (int) ((listH - thumbH) * (scroll / maxScroll));
            if (ui.lp && Ui.in(ui.mx, ui.my, sbx - 2, listY, 8, listH)) draggingBar = true;
            if (!ui.l) draggingBar = false;
            if (draggingBar) {
                double t = (ui.my - listY - thumbH / 2.0) / (listH - thumbH);
                scroll = Math.max(0, Math.min(1, t)) * maxScroll;
            }
            g.fill(sbx, thumbY, sbx + 4, thumbY + thumbH, Ui.argb(Ui.accent(), 0xFF));
        }

        // footer
        String info = list.size() + " / " + BlockCatalog.all().size() + " blocks";
        g.drawString(this.font, info, px + 6, py + ph - 15, Ui.TEXT_FAINT);
        if (ui.button(g, px + pw - 66, py + ph - 18, 60, 14, "Done", false)) {
            onClose();
            return;
        }

        if (this.getFocused() == null) this.setFocused(search);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void handleKeys() {
        long win = GLFW.glfwGetCurrentContext();
        int[] keys = {GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN};
        double[] delta = {-ROW, ROW, -listH, listH};
        for (int i = 0; i < keys.length; i++) {
            boolean d = GLFW.glfwGetKey(win, keys[i]) == GLFW.GLFW_PRESS;
            if (d && !prevKeys[keys[i]]) scroll += delta[i];
            prevKeys[keys[i]] = d;
        }
    }
}
