package dev.neonclient.module;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tutti i blocchi registrati (barrier, bedrock, portal frame, command block, ecc. inclusi). */
public final class BlockCatalog {
    public record Entry(String id, String shortName, Block block) {}

    private static List<Entry> all;
    private static Map<String, Block> byId;

    private BlockCatalog() {}

    private static void build() {
        if (all != null) return;
        List<Entry> list = new ArrayList<>();
        Map<String, Block> map = new HashMap<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(b).toString();
            String shortName = id.startsWith("minecraft:") ? id.substring(10) : id;
            list.add(new Entry(id, shortName, b));
            map.put(id, b);
        }
        list.sort((a, b) -> a.shortName().compareTo(b.shortName()));
        all = list;
        byId = map;
    }

    public static List<Entry> all() {
        build();
        return all;
    }

    public static Block byId(String id) {
        build();
        return byId.get(id);
    }

    /** Ricerca per nome: "red_sand" e "red sand" trovano entrambi red_sandstone. */
    public static List<Entry> filter(String query) {
        build();
        String q = query.toLowerCase().trim().replace(' ', '_');
        if (q.isEmpty()) return all;
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (e.id().contains(q)) out.add(e);
        }
        return out;
    }
}
