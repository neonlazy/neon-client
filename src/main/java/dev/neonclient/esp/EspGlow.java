package dev.neonclient.esp;

import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Decide quali entita' brillano e con che colore (Player / Entity / Item / Block ESP). */
public final class EspGlow {
    private EspGlow() {}

    /** Colore del glowing, oppure -1 se l'entita' non deve brillare per l'ESP. */
    /** True se l'entita' deve brillare (comprese le copie dei blocchi di Block ESP). */
    public static boolean shouldGlow(Entity e) {
        if (BlockGlow.isOurs(e)) return ModuleManager.BLOCK_ESP.enabled;
        return colorFor(e) >= 0;
    }

    public static int colorFor(Entity e) {
        if (e == Minecraft.getInstance().player) return -1;
        if (BlockGlow.isOurs(e)) return -1; // il colore dei blocchi e' gia' impostato sul display
        if (ModuleManager.PLAYER_ESP.enabled && e instanceof Player) return glow(ModuleManager.PLAYER_ESP.color);
        if (ModuleManager.ENTITY_ESP.enabled) {
            int c = ModuleManager.ENTITY_ESP.colorFor(e);
            if (c >= 0) return glow(c);
        }
        if (ModuleManager.ITEM_ESP.enabled) {
            int c = ModuleManager.ITEM_ESP.colorFor(e);
            if (c >= 0) return glow(c);
        }
        return -1;
    }

    /** Il colore 0 significa "nessun contorno", quindi il nero puro diventa quasi-nero. */
    private static int glow(int rgb) {
        rgb &= 0xFFFFFF;
        return rgb == 0 ? 0x010101 : rgb;
    }
}
