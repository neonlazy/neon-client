package dev.neonclient.esp;

import com.mojang.math.Transformation;
import dev.neonclient.mixin.BlockDisplayAccessor;
import dev.neonclient.mixin.DisplayAccessor;
import dev.neonclient.module.BlockEspModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Effetto glowing per i blocchi: su ogni blocco evidenziato viene messa un'entita' "block display"
 * che esiste solo sul client, con il contorno glowing (visibile attraverso i muri).
 * I blocchi senza modello (barrier, acqua, lava, light, aria...) non possono brillare:
 * per quelli l'ESP disegna un riquadro (vedi EspRenderer).
 */
public final class BlockGlow {
    private static final int FIRST_ID = -1_000_000;

    private static final Map<BlockPos, Display.BlockDisplay> DISPLAYS = new HashMap<>();
    private static final Map<BlockPos, BlockState> STATES = new HashMap<>();
    private static final Map<BlockPos, Integer> COLORS = new HashMap<>();
    private static final Set<Integer> IDS = new HashSet<>();
    private static ClientLevel lastLevel;
    private static int nextId = FIRST_ID;

    private BlockGlow() {}

    public static boolean isOurs(Entity e) {
        return e.getId() <= FIRST_ID && IDS.contains(e.getId());
    }

    /** True se il blocco ha un modello e quindi puo' brillare. */
    public static boolean canGlow(BlockState state) {
        return state.getRenderShape() != RenderShape.INVISIBLE || state.hasBlockEntity();
    }

    public static void sync(ClientLevel level, BlockPos[] positions, BlockEspModule module) {
        if (level != lastLevel) {
            DISPLAYS.clear();
            STATES.clear();
            COLORS.clear();
            IDS.clear();
            lastLevel = level;
        }

        Map<BlockPos, BlockState> want = new HashMap<>();
        for (BlockPos p : positions) {
            BlockState st = level.getBlockState(p);
            if (canGlow(st)) want.put(p, st);
        }

        Iterator<Map.Entry<BlockPos, Display.BlockDisplay>> it = DISPLAYS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Display.BlockDisplay> e = it.next();
            BlockState st = want.get(e.getKey());
            if (st == null || STATES.get(e.getKey()) != st) {
                remove(level, e.getValue());
                STATES.remove(e.getKey());
                COLORS.remove(e.getKey());
                it.remove();
            }
        }

        for (Map.Entry<BlockPos, BlockState> e : want.entrySet()) {
            int glow = glow(module.colorFor(e.getValue().getBlock()));
            Display.BlockDisplay d = DISPLAYS.get(e.getKey());
            if (d == null) {
                DISPLAYS.put(e.getKey(), create(level, e.getKey(), e.getValue(), glow));
                STATES.put(e.getKey(), e.getValue());
                COLORS.put(e.getKey(), glow);
            } else if (COLORS.getOrDefault(e.getKey(), -1) != glow) {
                ((DisplayAccessor) d).neon$setGlowColorOverride(glow);
                COLORS.put(e.getKey(), glow);
            }
        }
    }

    public static void clear() {
        if (lastLevel != null) {
            for (Display.BlockDisplay d : DISPLAYS.values()) remove(lastLevel, d);
        }
        DISPLAYS.clear();
        STATES.clear();
        COLORS.clear();
        IDS.clear();
    }

    private static int glow(int rgb) {
        rgb &= 0xFFFFFF;
        return rgb == 0 ? 0x010101 : rgb;
    }

    private static Display.BlockDisplay create(ClientLevel level, BlockPos pos, BlockState state, int glow) {
        Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        int id = nextId--;
        if (nextId < -2_000_000_000) nextId = FIRST_ID;
        d.setId(id);
        d.setPos(pos.getX(), pos.getY(), pos.getZ());
        ((BlockDisplayAccessor) d).neon$setBlockState(state);
        DisplayAccessor acc = (DisplayAccessor) d;
        acc.neon$setViewRange(16f);
        acc.neon$setGlowColorOverride(glow);
        // leggermente piu' grande del blocco vero, per non sovrapporsi esattamente alle sue facce
        acc.neon$setTransformation(new Transformation(new Vector3f(-0.002f), null, new Vector3f(1.004f), null));
        IDS.add(id);
        level.addEntity(d);
        return d;
    }

    private static void remove(ClientLevel level, Display.BlockDisplay d) {
        IDS.remove(d.getId());
        level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
    }
}
