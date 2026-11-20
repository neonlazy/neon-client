package dev.neonclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Xray: vengono disegnati solo i blocchi scelti (di default tutti i minerali), con luce piena.
 * Il resto del mondo diventa invisibile.
 */
public class XrayModule extends SelectionModule {
    private static final List<String> ORES = List.of(
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_quartz_ore", "minecraft:ancient_debris");

    /** Letto dai thread che costruiscono i chunk: si sostituisce sempre l'intero insieme. */
    private volatile Set<Block> visible = Set.of();

    public XrayModule() {
        super("Xray", Category.RENDER);
        selected.addAll(ORES);
    }

    @Override
    protected List<String> defaultSelection() {
        return ORES;
    }

    @Override
    public List<Catalog.Entry> catalog() {
        return Catalog.blocks();
    }

    @Override
    public String noun() {
        return "blocks";
    }

    public boolean isVisible(Block b) {
        return visible.contains(b);
    }

    private void rebuild() {
        Set<Block> s = new HashSet<>();
        for (String id : selected) {
            Block b = Catalog.block(id);
            if (b != null) s.add(b);
        }
        visible = Set.copyOf(s);
        dirty = false;
    }

    private static void reloadChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    @Override
    protected void onEnable() {
        rebuild();
        reloadChunks();
    }

    @Override
    protected void onDisable() {
        reloadChunks();
    }

    @Override
    public void onTick() {
        if (dirty) {
            rebuild();
            reloadChunks();
        }
    }
}
