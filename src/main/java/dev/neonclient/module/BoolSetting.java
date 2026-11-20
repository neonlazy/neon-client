package dev.neonclient.module;

public class BoolSetting {
    public final String name;
    public boolean value;
    public final boolean def;

    public BoolSetting(String name, boolean def) {
        this.name = name;
        this.value = def;
        this.def = def;
    }
}
