package dev.neonclient.module;

import dev.neonclient.esp.BlockGlow;
import dev.neonclient.esp.EspRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blocchi scelti con effetto glowing, letti dai dati dei chunk gia' ricevuti dal client:
 * funziona attraverso i muri e anche per zone non ancora disegnate.
 */
public class BlockEspModule extends SelectionModule {
    /** Numero massimo di blocchi evidenziati (i piu' vicini). */
    public static final int MAX_BLOCKS = 320;
    private static final int PER_CHUNK_CAP = 320;
    private static final int SCANS_PER_TICK = 4;
    private static final int RESCAN_TICKS = 100;

    public final NumberSetting range = new NumberSetting("Range (chunks)", 1, 16, 8, 1);
    public final BoolSetting tracers = new BoolSetting("Tracers", false);
    /** Evidenzia anche tutti i blocchi contenitore (chest, barrel, hopper, shulker...). */
    public final BoolSetting storage = new BoolSetting("Storage ESP", false);
    private boolean lastStorage;
    private static Set<Block> storageBlocks;

    private static final class ChunkData {
        List<BlockPos> list = new ArrayList<>();
        int tick;
    }

    private Set<Block> targets = new HashSet<>();
    private Map<Block, Integer> blockColors = new HashMap<>();
    private int cachedDefault = -1;
    private final Map<Long, ChunkData> cache = new HashMap<>();
    private BlockPos[] snapshot = new BlockPos[0];
    private int tickCount;

    public BlockEspModule() {
        super("Block ESP", Category.RENDER);
        hasColor = true;
        color = 0xFFFFFF;
        settings.add(range);
        bools.add(storage);
        bools.add(tracers);
    }

    @Override
    public List<Catalog.Entry> catalog() {
        return Catalog.blocks();
    }

    @Override
    public String noun() {
        return "blocks";
    }

    /** Blocchi evidenziati, dal piu' vicino. */
    public BlockPos[] snapshot() {
        return snapshot;
    }

    @Override
    protected void onEnable() {
        dirty = true;
    }

    @Override
    protected void onDisable() {
        cache.clear();
        snapshot = new BlockPos[0];
        BlockGlow.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            cache.clear();
            snapshot = new BlockPos[0];
            return;
        }
        tickCount++;

        if (dirty || cachedDefault != color || lastStorage != storage.value) {
            lastStorage = storage.value;
            Set<Block> t = new HashSet<>();
            if (storage.value) t.addAll(storageBlocks());
            Map<Block, Integer> cols = new HashMap<>();
            for (String id : selected) {
                Block b = Catalog.block(id);
                if (b != null) {
                    t.add(b);
                    cols.put(b, colorOf(id));
                }
            }
            blockColors = cols;
            cachedDefault = color;
            if (!t.equals(targets)) {
                targets = t;
                cache.clear();
                snapshot = new BlockPos[0];
            }
            dirty = false;
        }
        if (targets.isEmpty()) {
            BlockGlow.sync(level, snapshot, this);
            return;
        }

        double cx = EspRenderer.haveCam ? EspRenderer.camX : mc.player.getX();
        double cy = EspRenderer.haveCam ? EspRenderer.camY : mc.player.getY();
        double cz = EspRenderer.haveCam ? EspRenderer.camZ : mc.player.getZ();
        int pcx = Mth.floor(cx) >> 4;
        int pcz = Mth.floor(cz) >> 4;
        int r = (int) range.value;

        cache.keySet().removeIf(k -> {
            int kx = (int) (k >> 32);
            int kz = (int) (long) k;
            return Math.abs(kx - pcx) > r + 1 || Math.abs(kz - pcz) > r + 1;
        });

        List<int[]> todo = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                ChunkData d = cache.get(key(pcx + dx, pcz + dz));
                if (d == null || tickCount - d.tick > RESCAN_TICKS) {
                    todo.add(new int[]{pcx + dx, pcz + dz, dx * dx + dz * dz});
                }
            }
        }
        todo.sort((a, b) -> Integer.compare(a[2], b[2]));
        int minY = level.dimensionType().minY();
        for (int i = 0; i < Math.min(SCANS_PER_TICK, todo.size()); i++) {
            int[] c = todo.get(i);
            ChunkData d = new ChunkData();
            ChunkAccess chunk = level.getChunk(c[0], c[1], ChunkStatus.FULL, false);
            if (chunk == null) {
                d.tick = tickCount - (RESCAN_TICKS - 20); // riprova tra ~1 secondo
            } else {
                d.tick = tickCount;
                scan(chunk, c[0], c[1], minY, d.list);
            }
            cache.put(key(c[0], c[1]), d);
        }

        if (tickCount % 10 == 0) rebuildSnapshot(level, cx, cy, cz);
        BlockGlow.sync(level, snapshot, this);
    }

    /** Blocchi contenitore: tutte le chest (anche copper, ender, trapped), shulker box, barrel, hopper... */
    public static Set<Block> storageBlocks() {
        if (storageBlocks == null) {
            Set<String> extra = Set.of("minecraft:barrel", "minecraft:hopper", "minecraft:dropper",
                    "minecraft:dispenser", "minecraft:crafter", "minecraft:decorated_pot", "minecraft:furnace",
                    "minecraft:blast_furnace", "minecraft:smoker", "minecraft:brewing_stand");
            Set<Block> s = new HashSet<>();
            for (Catalog.Entry e : Catalog.blocks()) {
                String id = e.id();
                if (id.endsWith("chest") || id.endsWith("shulker_box") || extra.contains(id)) {
                    Block b = Catalog.block(id);
                    if (b != null) s.add(b);
                }
            }
            storageBlocks = s;
        }
        return storageBlocks;
    }

    /** Colore con cui evidenziare un blocco (personalizzato o di default). */
    public int colorFor(Block b) {
        Integer c = blockColors.get(b);
        return c != null ? c : color;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private void scan(ChunkAccess chunk, int cx, int cz, int minY, List<BlockPos> out) {
        boolean airTarget = targets.contains(Blocks.AIR) || targets.contains(Blocks.CAVE_AIR)
                || targets.contains(Blocks.VOID_AIR);
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection s = sections[i];
            if (s == null) continue;
            if (!airTarget && s.hasOnlyAir()) continue;
            if (!s.maybeHas(st -> targets.contains(st.getBlock()))) continue;
            int baseY = minY + i * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState st = s.getBlockState(x, y, z);
                        if (targets.contains(st.getBlock())) {
                            out.add(new BlockPos(cx * 16 + x, baseY + y, cz * 16 + z));
                            if (out.size() >= PER_CHUNK_CAP) return;
                        }
                    }
                }
            }
        }
    }

    private void rebuildSnapshot(ClientLevel level, double cx, double cy, double cz) {
        List<BlockPos> all = new ArrayList<>();
        for (ChunkData d : cache.values()) all.addAll(d.list);
        all.sort((a, b) -> Double.compare(dist2(a, cx, cy, cz), dist2(b, cx, cy, cz)));
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : all) {
            // scarta i blocchi rotti/cambiati dall'ultima scansione
            if (!targets.contains(level.getBlockState(p).getBlock())) continue;
            out.add(p);
            if (out.size() >= MAX_BLOCKS) break;
        }
        snapshot = out.toArray(new BlockPos[0]);
    }

    private static double dist2(BlockPos p, double cx, double cy, double cz) {
        double dx = p.getX() + 0.5 - cx, dy = p.getY() + 0.5 - cy, dz = p.getZ() + 0.5 - cz;
        return dx * dx + dy * dy + dz * dz;
    }
}
