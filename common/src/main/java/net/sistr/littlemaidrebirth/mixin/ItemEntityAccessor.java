package net.sistr.littlemaidrebirth.mixin;

import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
  @Accessor
  UUID getOwner();
}
