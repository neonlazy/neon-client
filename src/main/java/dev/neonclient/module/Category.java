package dev.neonclient.module;

/** Categorie in ordine alfabetico; Favorites resta in fondo (la colonna Search e' gestita dalla GUI). */
public enum Category {
    COMBAT("Combat"),
    MISC("Misc"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    WORLD("World"),
    FAVORITES("Favorites");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
