package dev.neonclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.neonclient.module.BoolSetting;
import dev.neonclient.module.Module;
import dev.neonclient.module.ModuleManager;
import dev.neonclient.module.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NeonConfig {
    public enum Style {
        SOLID("Solid"), CONSTELLATION("Constellation"), GRADIENT("Gradient");
        public final String label;
        Style(String label) { this.label = label; }
    }

    /** Stile dei pannelli dei moduli (la parte scura). */
    public enum PanelStyle {
        SOLID("Solid"), GRADIENT("Gradient"), GLASS("Glass");
        public final String label;
        PanelStyle(String label) { this.label = label; }
    }

    /** Carattere della GUI. */
    public enum FontChoice {
        MINECRAFT("Minecraft"), DEFAULT("Default"), VANILLA_PLUS("Vanilla+");
        public final String label;
        FontChoice(String label) { this.label = label; }
    }

    static class ModData {
        int key;
        boolean favorite;
        int color = -1;
        java.util.List<String> blocks;
        Map<String, Integer> colors;
        Map<String, Double> settings = new HashMap<>();
    }

    static class Data {
        String style = Style.SOLID.name();
        int color1 = 0xA020F0;
        int color2 = 0x2B0A4F;
        String panelStyle = PanelStyle.SOLID.name();
        String font = FontChoice.MINECRAFT.name();
        int panelColor1 = 0x0B0912;
        int panelColor2 = 0x2A0F45;
        Map<String, ModData> modules = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("neonclient.json");
    private static Data data = new Data();

    public static void load() {
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) data = d;
        } catch (Exception e) {
            System.err.println("[NeonClient] Impossibile leggere la config: " + e);
        }
    }

    public static void save() {
        for (Module m : ModuleManager.all()) {
            ModData md = data.modules.computeIfAbsent(m.id(), k -> new ModData());
            md.key = m.key;
            md.favorite = m.favorite;
            md.color = m.hasColor ? m.color : -1;
            md.blocks = m.getList();
            md.colors = m.getColorMap();
            for (NumberSetting s : m.settings) md.settings.put(s.name, s.value);
            for (BoolSetting b : m.bools) md.settings.put("bool:" + b.name, b.value ? 1.0 : 0.0);
        }
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(data, w);
        } catch (IOException e) {
            System.err.println("[NeonClient] Impossibile salvare la config: " + e);
        }
    }

    public static void applyTo(Module m) {
        ModData md = data.modules.get(m.id());
        if (md == null) return;
        m.key = md.key;
        m.favorite = md.favorite;
        if (m.hasColor && md.color >= 0) m.color = md.color & 0xFFFFFF;
        if (md.blocks != null) m.setList(md.blocks);
        if (md.colors != null) m.setColorMap(md.colors);
        for (NumberSetting s : m.settings) {
            Double v = md.settings.get(s.name);
            if (v != null) s.set(v);
        }
        for (BoolSetting b : m.bools) {
            Double v = md.settings.get("bool:" + b.name);
            if (v != null) b.value = v != 0;
        }
    }

    public static Style style() {
        try {
            return Style.valueOf(data.style);
        } catch (Exception e) {
            return Style.SOLID;
        }
    }

    public static void setStyle(Style s) { data.style = s.name(); }
    public static int color1() { return data.color1 & 0xFFFFFF; }
    public static int color2() { return data.color2 & 0xFFFFFF; }
    public static void setColor1(int rgb) { data.color1 = rgb & 0xFFFFFF; }
    public static void setColor2(int rgb) { data.color2 = rgb & 0xFFFFFF; }

    public static PanelStyle panelStyle() {
        try {
            return PanelStyle.valueOf(data.panelStyle);
        } catch (Exception e) {
            return PanelStyle.SOLID;
        }
    }

    public static void setPanelStyle(PanelStyle s) { data.panelStyle = s.name(); }
    public static int panelColor1() { return data.panelColor1 & 0xFFFFFF; }
    public static int panelColor2() { return data.panelColor2 & 0xFFFFFF; }
    public static void setPanelColor1(int rgb) { data.panelColor1 = rgb & 0xFFFFFF; }
    public static void setPanelColor2(int rgb) { data.panelColor2 = rgb & 0xFFFFFF; }

    public static FontChoice font() {
        try {
            return FontChoice.valueOf(data.font);
        } catch (Exception e) {
            return FontChoice.MINECRAFT;
        }
    }

    public static void setFont(FontChoice f) { data.font = f.name(); }
}
