package dev.neonclient.module;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Elenchi completi di blocchi, entita' e oggetti del gioco, per i selettori degli ESP. */
public final class Catalog {
    public record Entry(String id, String shortName, Supplier<String> name, Supplier<ItemStack> icon) {}

    private static List<Entry> blocks, entities, items;
    private static final Map<String, Block> BLOCKS = new HashMap<>();
    private static final Map<String, EntityType<?>> ENTITIES = new HashMap<>();
    private static final Map<String, Item> ITEMS = new HashMap<>();

    private Catalog() {}

    private static String shortName(String id) {
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    private static void sort(List<Entry> list) {
        list.sort((a, b) -> a.shortName().compareTo(b.shortName()));
    }

    /** Tutti i blocchi registrati, compresi barrier, bedrock, end portal frame, command block... */
    public static List<Entry> blocks() {
        if (blocks == null) {
            List<Entry> list = new ArrayList<>();
            for (Block b : BuiltInRegistries.BLOCK) {
                String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                BLOCKS.put(id, b);
                list.add(new Entry(id, shortName(id), () -> b.getName().getString(), () -> new ItemStack(b)));
            }
            sort(list);
            blocks = Collections.unmodifiableList(list);
        }
        return blocks;
    }

    /** Tutti i tipi di entita' tranne il giocatore (gestito da Player ESP). */
    public static List<Entry> entities() {
        if (entities == null) {
            items();
            List<Entry> list = new ArrayList<>();
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (t == EntityType.PLAYER) continue;
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
                ENTITIES.put(id, t);
                Item egg = ITEMS.get(id + "_spawn_egg");
                list.add(new Entry(id, shortName(id), () -> t.getDescription().getString(),
                        () -> egg != null ? new ItemStack(egg) : ItemStack.EMPTY));
            }
            sort(list);
            entities = Collections.unmodifiableList(list);
        }
        return entities;
    }

    /** Tutti gli oggetti (per gli item droppati). */
    public static List<Entry> items() {
        if (items == null) {
            List<Entry> list = new ArrayList<>();
            for (Item it : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(it).toString();
                ITEMS.put(id, it);
                if (it == Items.AIR) continue;
                list.add(new Entry(id, shortName(id), () -> new ItemStack(it).getHoverName().getString(),
                        () -> new ItemStack(it)));
            }
            sort(list);
            items = Collections.unmodifiableList(list);
        }
        return items;
    }

    public static Block block(String id) {
        blocks();
        return BLOCKS.get(id);
    }

    public static EntityType<?> entity(String id) {
        entities();
        return ENTITIES.get(id);
    }

    public static Item item(String id) {
        items();
        return ITEMS.get(id);
    }
}
