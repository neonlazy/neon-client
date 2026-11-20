package dev.neonclient.module;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evidenzia i blocchi scelti leggendo i dati dei chunk gia' ricevuti dal client:
 * funziona attraverso i muri e anche per sezioni non ancora disegnate.
 */
public class BlockEspModule extends Module {
    private static final int PER_CHUNK_CAP = 150;
    private static final int SNAPSHOT_CAP = 3000;
    private static final int SCANS_PER_TICK = 4;
    private static final int RESCAN_TICKS = 100;

    public final NumberSetting range = new NumberSetting("Range (chunks)", 1, 16, 8, 1);
    public final Set<String> selected = new LinkedHashSet<>();

    private static final class ChunkData {
        List<BlockPos> list = new ArrayList<>();
        int tick;
    }

    private Set<Block> targets = new HashSet<>();
    private boolean dirty = true;
    private final Map<Long, ChunkData> cache = new HashMap<>();
    private BlockPos[] snapshot = new BlockPos[0];
    private int tickCount;

    public BlockEspModule() {
        super("Block ESP", Category.RENDER);
        hasColor = true;
        color = 0xFFFFFF;
        settings.add(range);
    }

    public BlockPos[] snapshot() {
        return snapshot;
    }

    public boolean isSelected(String id) {
        return selected.contains(id);
    }

    public void toggleBlock(String id) {
        if (!selected.remove(id)) selected.add(id);
        dirty = true;
    }

    public void clearSelection() {
        selected.clear();
        dirty = true;
    }

    @Override
    public List<String> getList() {
        return new ArrayList<>(selected);
    }

    @Override
    public void setList(List<String> list) {
        selected.clear();
        selected.addAll(list);
        dirty = true;
    }

    @Override
    protected void onEnable() {
        dirty = true;
    }

    @Override
    protected void onDisable() {
        cache.clear();
        snapshot = new BlockPos[0];
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

        if (dirty) {
            Set<Block> t = new HashSet<>();
            for (String id : selected) {
                Block b = BlockCatalog.byId(id);
                if (b != null) t.add(b);
            }
            targets = t;
            cache.clear();
            snapshot = new BlockPos[0];
            dirty = false;
        }
        if (targets.isEmpty()) return;

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

        if (tickCount % 10 == 0) rebuildSnapshot(cx, cy, cz);
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

    private void rebuildSnapshot(double cx, double cy, double cz) {
        List<BlockPos> all = new ArrayList<>();
        for (ChunkData d : cache.values()) all.addAll(d.list);
        all.sort((a, b) -> Double.compare(dist2(a, cx, cy, cz), dist2(b, cx, cy, cz)));
        int n = Math.min(SNAPSHOT_CAP, all.size());
        snapshot = all.subList(0, n).toArray(new BlockPos[0]);
    }

    private static double dist2(BlockPos p, double cx, double cy, double cz) {
        double dx = p.getX() + 0.5 - cx, dy = p.getY() + 0.5 - cy, dz = p.getZ() + 0.5 - cz;
        return dx * dx + dy * dy + dz * dz;
    }
}
