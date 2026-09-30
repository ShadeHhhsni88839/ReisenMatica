package io.github.shade.reisenmatica.mixin;

import net.minecraft.client.render.WorldRenderer;
import net.mymod.reisenmatica.Projection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class ProjectionMixin {

    @Inject(method = "renderEntities", at = @At("RETURN"))
    private void reisenmatica$renderProjection(CallbackInfo ci) {
        Projection.render();
    }
}