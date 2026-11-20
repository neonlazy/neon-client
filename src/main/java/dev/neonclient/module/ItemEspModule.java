package dev.neonclient.module;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Oggetti droppati (tutti o quelli scelti) con effetto glowing permanente + linee tracker opzionali. */
public class ItemEspModule extends SelectionModule {
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    private Set<Item> itemSet = new HashSet<>();

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

    public boolean matches(Entity e) {
        if (!(e instanceof ItemEntity item)) return false;
        if (dirty) {
            Set<Item> s = new HashSet<>();
            for (String id : selected) {
                Item it = Catalog.item(id);
                if (it != null) s.add(it);
            }
            itemSet = s;
            dirty = false;
        }
        return itemSet.isEmpty() || itemSet.contains(item.getItem().getItem());
    }
}
