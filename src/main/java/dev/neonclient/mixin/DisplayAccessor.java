package dev.neonclient.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setViewRange")
    void neon$setViewRange(float range);

    @Invoker("setGlowColorOverride")
    void neon$setGlowColorOverride(int color);

    @Invoker("setTransformation")
    void neon$setTransformation(Transformation transformation);
}
