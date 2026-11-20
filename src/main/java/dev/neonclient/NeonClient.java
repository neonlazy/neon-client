package dev.neonclient;

import dev.neonclient.gui.NeonScreen;
import dev.neonclient.module.Module;
import dev.neonclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class NeonClient implements ClientModInitializer {
    public static final String NAME = "Neon Client";

    private static boolean prevOpenKey;
    private static String version = "?";

    public static String version() {
        return version;
    }

    @Override
    public void onInitializeClient() {
        version = FabricLoader.getInstance().getModContainer("neonclient")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        NeonConfig.load();
        ModuleManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(NeonClient::tick);
    }

    private static void tick(Minecraft mc) {
        long win = GLFW.glfwGetCurrentContext();
        if (win == 0L) return;

        boolean openDown = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean canUseKeys = mc.screen == null && mc.player != null && mc.level != null;

        if (canUseKeys && openDown && !prevOpenKey) {
            mc.setScreen(new NeonScreen());
        }
        prevOpenKey = openDown;

        for (Module m : ModuleManager.all()) {
            if (m.key <= 0) {
                m.keyWasDown = false;
                continue;
            }
            boolean down = GLFW.glfwGetKey(win, m.key) == GLFW.GLFW_PRESS;
            if (canUseKeys && down && !m.keyWasDown) m.toggle();
            m.keyWasDown = down;
        }

        ModuleManager.tick();
    }
}
