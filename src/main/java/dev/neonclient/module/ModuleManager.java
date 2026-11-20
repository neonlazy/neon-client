package dev.neonclient.module;

import dev.neonclient.NeonConfig;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final FreecamModule FREECAM = new FreecamModule();
    public static final PlayerEspModule PLAYER_ESP = new PlayerEspModule();
    public static final BlockEspModule BLOCK_ESP = new BlockEspModule();

    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.clear();
        // Movement, Combat, World, Player, Misc: vuote per ora.
        MODULES.add(FREECAM);
        MODULES.add(PLAYER_ESP);
        MODULES.add(BLOCK_ESP);
        for (Module m : MODULES) NeonConfig.applyTo(m);
    }

    public static List<Module> all() {
        return MODULES;
    }

    public static List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) {
            if (c == Category.FAVORITES ? m.favorite : m.category == c) out.add(m);
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    public static void tick() {
        for (Module m : new ArrayList<>(MODULES)) {
            if (m.enabled) m.onTick();
        }
    }
}
