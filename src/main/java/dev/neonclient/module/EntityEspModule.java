package dev.neonclient.module;

import dev.neonclient.esp.BlockGlow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Mob (o le entita' scelte) con effetto glowing permanente + linee tracker opzionali. */
public class EntityEspModule extends SelectionModule {
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    private Set<EntityType<?>> types = new HashSet<>();

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

    public boolean matches(Entity e) {
        if (e instanceof Player || BlockGlow.isOurs(e)) return false;
        if (dirty) {
            Set<EntityType<?>> t = new HashSet<>();
            for (String id : selected) {
                EntityType<?> type = Catalog.entity(id);
                if (type != null) t.add(type);
            }
            types = t;
            dirty = false;
        }
        if (types.isEmpty()) return e instanceof Mob;
        return types.contains(e.getType());
    }
}
