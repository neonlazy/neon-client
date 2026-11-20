package dev.neonclient.module;

import dev.neonclient.NeonConfig;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final BlockEspModule BLOCK_ESP = new BlockEspModule();
    public static final EntityEspModule ENTITY_ESP = new EntityEspModule();
    public static final FreecamModule FREECAM = new FreecamModule();
    public static final ItemEspModule ITEM_ESP = new ItemEspModule();
    public static final PlayerEspModule PLAYER_ESP = new PlayerEspModule();
    public static final XrayModule XRAY = new XrayModule();

    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.clear();
        // Combat, Misc, Movement, Player, World: vuote per ora.
        MODULES.add(BLOCK_ESP);
        MODULES.add(ENTITY_ESP);
        MODULES.add(FREECAM);
        MODULES.add(ITEM_ESP);
        MODULES.add(PLAYER_ESP);
        MODULES.add(XRAY);
        MODULES.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
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

    /** Moduli il cui nome contiene il testo cercato (in ordine alfabetico). */
    public static List<Module> search(String query) {
        List<Module> out = new ArrayList<>();
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) return out;
        for (Module m : MODULES) {
            if (m.name.toLowerCase().contains(q)) out.add(m);
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
