package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.ServerAdvancementLoader;
import net.sistr.littlemaidrebirth.util.PlayerAdvancementTrackerWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancementTracker.class)
public class MixinPlayerAdvancementTracker implements PlayerAdvancementTrackerWrapper {

    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private void onLoad(ServerAdvancementLoader advancementLoader, CallbackInfo ci) {
        if (isNonFileAdvancement()) {
            ci.cancel();
        }
    }

    @Override
    public boolean isNonFileAdvancement() {
        return false;
    }

}
