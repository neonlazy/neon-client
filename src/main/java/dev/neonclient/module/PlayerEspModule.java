package dev.neonclient.module;

/** Evidenzia i giocatori attraverso i muri (disegno in overlay, vedi EspRenderer). */
public class PlayerEspModule extends Module {
    public PlayerEspModule() {
        super("Player ESP", Category.RENDER);
        hasColor = true;
        color = 0xFFFFFF;
    }
}
