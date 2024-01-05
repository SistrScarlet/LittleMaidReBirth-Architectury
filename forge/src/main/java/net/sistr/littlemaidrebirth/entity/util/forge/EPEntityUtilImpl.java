package net.sistr.littlemaidrebirth.entity.util.forge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

public class EPEntityUtilImpl {
    public static PersistentProjectileEntity arrowCustomHook(BowItem bowItem, PersistentProjectileEntity projectile) {
        return bowItem.customArrow(projectile);
    }

    public static ItemStack arrowCustomHook(LivingEntity user, ItemStack weapon, ItemStack arrow) {
        return arrow;
    }
}
