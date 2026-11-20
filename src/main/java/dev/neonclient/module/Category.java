package dev.neonclient.module;

public enum Category {
    MOVEMENT("Movement"),
    COMBAT("Combat"),
    WORLD("World"),
    PLAYER("Player"),
    RENDER("Render"),
    MISC("Misc"),
    FAVORITES("Favorites");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
