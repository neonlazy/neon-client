package dev.neonclient.gui;

import dev.neonclient.NeonConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Carattere della GUI scelto in Customize:
 * Minecraft (vanilla), Default (Inter, pulito) e Vanilla+ (Pixelify Sans, pixel morbido).
 */
public final class NeonFont {
    private static final FontDescription INTER =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("neonclient", "inter"));
    private static final FontDescription PIXELIFY =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("neonclient", "pixelify"));

    private NeonFont() {}

    public static Component of(String text) {
        MutableComponent c = Component.literal(text);
        return switch (NeonConfig.font()) {
            case DEFAULT -> c.withStyle(s -> s.withFont(INTER));
            case VANILLA_PLUS -> c.withStyle(s -> s.withFont(PIXELIFY));
            default -> c;
        };
    }
}
