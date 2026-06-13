package net.sistr.littlemaidrebirth.mixin;

import java.util.Optional;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.LMCollidable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ExperienceOrbEntity.class)
public abstract class MixinExperienceOrbEntity extends Entity implements LMCollidable {

  @Shadow private int pickingCount;

  @Shadow private int amount;

  public MixinExperienceOrbEntity(EntityType<?> type, World world) {
    super(type, world);
  }

  @Override
  public void onCollision_LMRB(LittleMaidEntity maid) {
    if (this.getWorld().isClient || maid.experiencePickUpDelay != 0) {
      return;
    }
    maid.experiencePickUpDelay = 2;
    maid.sendPickup(this, 1);
    int i = this.repairGears_LM(maid, this.amount);
    if (i > 0) {
      maid.addExperience(i);
    }
    --this.pickingCount;
    if (this.pickingCount == 0) {
      this.discard();
    }
  }

  @Unique
  private int repairGears_LM(LittleMaidEntity littleMaid, int amount) {
    Optional<EnchantmentEffectContext> optional =
        EnchantmentHelper.chooseEquipmentWith(
            EnchantmentEffectComponentTypes.REPAIR_WITH_XP, littleMaid, ItemStack::isDamaged);
    if (optional.isEmpty()) {
      return amount;
    }
    ItemStack itemStack = optional.get().stack();
    ServerWorld serverWorld = (ServerWorld) littleMaid.getWorld();
    int repairWithXp = EnchantmentHelper.getRepairWithXp(serverWorld, itemStack, amount);
    int repaired = Math.min(repairWithXp, itemStack.getDamage());
    itemStack.setDamage(itemStack.getDamage() - repaired);
    if (repaired > 0) {
      int remaining = amount - repaired * amount / repairWithXp;
      if (remaining > 0) {
        return this.repairGears_LM(littleMaid, remaining);
      }
    }
    return 0;
  }
}
