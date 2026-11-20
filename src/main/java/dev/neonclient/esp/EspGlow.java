package dev.neonclient.esp;

import dev.neonclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Decide quali entita' brillano e con che colore (Player / Entity / Item / Block ESP). */
public final class EspGlow {
    private EspGlow() {}

    /** Colore del glowing, oppure -1 se l'entita' non deve brillare per l'ESP. */
    public static int colorFor(Entity e) {
        if (e == Minecraft.getInstance().player) return -1;
        if (BlockGlow.isOurs(e)) return ModuleManager.BLOCK_ESP.enabled ? glow(ModuleManager.BLOCK_ESP.color) : -1;
        if (ModuleManager.PLAYER_ESP.enabled && e instanceof Player) return glow(ModuleManager.PLAYER_ESP.color);
        if (ModuleManager.ENTITY_ESP.enabled && ModuleManager.ENTITY_ESP.matches(e)) return glow(ModuleManager.ENTITY_ESP.color);
        if (ModuleManager.ITEM_ESP.enabled && ModuleManager.ITEM_ESP.matches(e)) return glow(ModuleManager.ITEM_ESP.color);
        return -1;
    }

    /** Il colore 0 significa "nessun contorno", quindi il nero puro diventa quasi-nero. */
    private static int glow(int rgb) {
        rgb &= 0xFFFFFF;
        return rgb == 0 ? 0x010101 : rgb;
    }
}
