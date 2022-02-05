package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity {

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void onCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.setIFFs(((HasIFF) oldPlayer).getIFFs());
    }

}
