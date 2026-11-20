package dev.neonclient.module;

import dev.neonclient.esp.BlockGlow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mob (o le entita' scelte) con effetto glowing permanente + linee tracker opzionali. */
public class EntityEspModule extends SelectionModule {
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    private Map<EntityType<?>, Integer> types = new HashMap<>();
    private int cachedDefault = -1;

    public EntityEspModule() {
        super("Entity ESP", Category.RENDER);
        hasColor = true;
        color = 0xFFFFFF;
        bools.add(tracers);
    }

    @Override
    public List<Catalog.Entry> catalog() {
        return Catalog.entities();
    }

    @Override
    public String noun() {
        return "mobs";
    }

    @Override
    public boolean emptyMeansAll() {
        return true;
    }

    /** Colore dell'entita', oppure -1 se non va evidenziata. */
    public int colorFor(Entity e) {
        if (e instanceof Player || BlockGlow.isOurs(e)) return -1;
        if (dirty || cachedDefault != color) {
            Map<EntityType<?>, Integer> t = new HashMap<>();
            for (String id : selected) {
                EntityType<?> type = Catalog.entity(id);
                if (type != null) t.put(type, colorOf(id));
            }
            types = t;
            cachedDefault = color;
            dirty = false;
        }
        if (types.isEmpty()) return e instanceof Mob ? color : -1;
        Integer c = types.get(e.getType());
        return c != null ? c : -1;
    }
}
