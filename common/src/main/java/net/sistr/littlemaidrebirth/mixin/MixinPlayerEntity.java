package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.sistr.littlemaidrebirth.util.PlayerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity implements PlayerAccessor {

    @Shadow protected abstract void collideWithEntity(Entity entity);

    @Override
    public void onCollideWithEntity_LM(Entity entity) {
        collideWithEntity(entity);
    }
}
