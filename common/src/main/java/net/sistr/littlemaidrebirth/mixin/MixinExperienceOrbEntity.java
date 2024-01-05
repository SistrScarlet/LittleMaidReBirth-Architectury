package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.LMCollidable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(ExperienceOrbEntity.class)
public abstract class MixinExperienceOrbEntity extends Entity implements LMCollidable {

    @Shadow
    private int amount;

    public MixinExperienceOrbEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    protected abstract int getMendingRepairCost(int repairAmount);

    @Shadow
    protected abstract int getMendingRepairAmount(int experienceAmount);

    @Override
    public void onCollision_LMRB(LittleMaidEntity littleMaid) {
        if (!this.getEntityWorld().isClient) {
            if (littleMaid.experiencePickUpDelay == 0) {
                littleMaid.experiencePickUpDelay = 2;
                littleMaid.sendPickup(this, 1);
                int i = this.repairGears_LM(littleMaid, this.amount);
                if (i > 0) {
                    littleMaid.addExperience(i);
                }

                this.remove();
            }
        }
    }

    @Unique
    private int repairGears_LM(LittleMaidEntity littleMaid, int amount) {
        Map.Entry<EquipmentSlot, ItemStack> entry = EnchantmentHelper.chooseEquipmentWith(Enchantments.MENDING, littleMaid, ItemStack::isDamaged);
        if (entry != null) {
            ItemStack itemStack = entry.getValue();
            int i = Math.min(this.getMendingRepairAmount(this.amount), itemStack.getDamage());
            itemStack.setDamage(itemStack.getDamage() - i);
            int j = amount - this.getMendingRepairCost(i);
            return j > 0 ? this.repairGears_LM(littleMaid, j) : 0;
        } else {
            return amount;
        }
    }
}
