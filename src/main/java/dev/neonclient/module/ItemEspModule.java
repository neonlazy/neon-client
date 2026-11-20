package dev.neonclient.module;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Oggetti droppati (tutti o quelli scelti) con effetto glowing permanente + linee tracker opzionali. */
public class ItemEspModule extends SelectionModule {
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    private Map<Item, Integer> itemColors = new HashMap<>();
    private int cachedDefault = -1;

    public ItemEspModule() {
        super("Item ESP", Category.RENDER);
        hasColor = true;
        color = 0xFFFFFF;
        bools.add(tracers);
    }

    @Override
    public List<Catalog.Entry> catalog() {
        return Catalog.items();
    }

    @Override
    public String noun() {
        return "items";
    }

    @Override
    public boolean emptyMeansAll() {
        return true;
    }

    /** Colore dell'oggetto droppato, oppure -1 se non va evidenziato. */
    public int colorFor(Entity e) {
        if (!(e instanceof ItemEntity item)) return -1;
        if (dirty || cachedDefault != color) {
            Map<Item, Integer> m = new HashMap<>();
            for (String id : selected) {
                Item it = Catalog.item(id);
                if (it != null) m.put(it, colorOf(id));
            }
            itemColors = m;
            cachedDefault = color;
            dirty = false;
        }
        if (itemColors.isEmpty()) return color;
        Integer c = itemColors.get(item.getItem().getItem());
        return c != null ? c : -1;
    }
}
