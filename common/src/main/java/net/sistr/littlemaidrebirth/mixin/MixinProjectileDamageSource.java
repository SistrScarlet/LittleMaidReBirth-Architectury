package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.sistr.littlemaidrebirth.entity.FakePlayerWrapperEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileDamageSource.class)
public class MixinProjectileDamageSource {

    @Mutable
    @Shadow @Final @Nullable private Entity attacker;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(String name, Entity projectile, Entity attacker, CallbackInfo ci) {
        if (attacker instanceof FakePlayerWrapperEntity) {
            this.attacker = ((FakePlayerWrapperEntity<?>) attacker).getOrigin();
        }
    }

}
