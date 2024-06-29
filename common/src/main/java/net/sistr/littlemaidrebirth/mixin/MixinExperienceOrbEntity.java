package net.sistr.littlemaidrebirth.mixin;

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

import java.util.Optional;

@Mixin(ExperienceOrbEntity.class)
public abstract class MixinExperienceOrbEntity extends Entity implements LMCollidable {

    @Shadow
    private int pickingCount;

    @Shadow
    private int amount;

    public MixinExperienceOrbEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void onCollision_LMRB(LittleMaidEntity littleMaid) {
        if (!this.getWorld().isClient) {
            if (littleMaid.experiencePickUpDelay == 0) {
                littleMaid.experiencePickUpDelay = 2;
                littleMaid.sendPickup(this, 1);
                int i = this.repairGears_LM(littleMaid, this.amount);
                if (i > 0) {
                    littleMaid.addExperience(i);
                }

                --this.pickingCount;
                if (this.pickingCount == 0) {
                    this.discard();
                }
            }
        }
    }

    //todo 仕様変更あり
    @Unique
    private int repairGears_LM(LittleMaidEntity littleMaid, int amount) {
        Optional<EnchantmentEffectContext> optional = EnchantmentHelper.chooseEquipmentWith(EnchantmentEffectComponentTypes.REPAIR_WITH_XP, littleMaid, ItemStack::isDamaged);
        if (optional.isPresent()) {
            int k;
            ItemStack itemStack = optional.get().stack();
            int i = EnchantmentHelper.getRepairWithXp((ServerWorld) littleMaid.getWorld(), itemStack, amount);
            int j = Math.min(i, itemStack.getDamage());
            itemStack.setDamage(itemStack.getDamage() - j);
            if (j > 0 && (k = amount - j * amount / i) > 0) {
                return repairGears_LM(littleMaid, k);
            }
            return 0;
        }
        return amount;
    }
}
