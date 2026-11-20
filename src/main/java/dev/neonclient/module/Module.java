package dev.neonclient.module;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final Category category;
    public final List<NumberSetting> settings = new ArrayList<>();
    public final List<BoolSetting> bools = new ArrayList<>();

    public boolean enabled;
    public boolean favorite;
    /** GLFW key code, 0 = nessun tasto associato. */
    public int key;
    public boolean keyWasDown;

    /** Se true la finestra impostazioni mostra il selettore colore. */
    public boolean hasColor;
    public int color = 0xFFFFFF;
    protected int defaultColor = 0xFFFFFF;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public String id() {
        return name.toLowerCase().replace(' ', '_');
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}

    /** Riporta il modulo alle impostazioni originali (spento, senza tasto, non preferito). */
    public void resetToDefaults() {
        setEnabled(false);
        key = 0;
        favorite = false;
        color = defaultColor;
        for (NumberSetting s : settings) s.value = s.def;
        for (BoolSetting b : bools) b.value = b.def;
    }

    /** Lista extra da salvare in config (usata da Block ESP). */
    public List<String> getList() { return null; }
    public void setList(List<String> list) {}

    /** Colori per singolo elemento (usata dagli ESP con selettore). */
    public java.util.Map<String, Integer> getColorMap() { return null; }
    public void setColorMap(java.util.Map<String, Integer> map) {}
}
