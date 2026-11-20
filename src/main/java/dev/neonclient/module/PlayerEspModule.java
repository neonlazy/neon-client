package dev.neonclient.module;

/** Giocatori con effetto glowing permanente (visibile attraverso i muri) + linee tracker opzionali. */
public class PlayerEspModule extends Module {
    public final BoolSetting tracers = new BoolSetting("Tracers", false);

    public PlayerEspModule() {
        super("Player ESP", Category.RENDER);
        hasColor = true;
        color = 0xFFFFFF;
        bools.add(tracers);
    }

    /** Colore del contorno glowing (0 = nessun contorno, quindi si evita il nero puro). */
    public int glowColor() {
        int c = color & 0xFFFFFF;
        return c == 0 ? 0x010101 : c;
    }
}
