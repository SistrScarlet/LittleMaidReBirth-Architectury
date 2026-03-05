package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.sistr.littlemaidrebirth.util.BrewingStandAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BrewingStandBlockEntity.class)
public abstract class MixinBrewingStandBlockEntity implements BrewingStandAccessor {

  @Shadow int brewTime;

  @Shadow int fuel;

  @Override
  public int getBrewTime_LM() {
    return this.brewTime;
  }

  @Override
  public int getFuel_LM() {
    return this.fuel;
  }
}
