package net.sistr.littlemaidrebirth.entity.util.fabric;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

public class EPEntityUtilImpl {
    public static PersistentProjectileEntity arrowCustomHook(BowItem bowItem, PersistentProjectileEntity projectile) {
        return projectile;
    }

    public static ItemStack arrowCustomHook(LivingEntity user, ItemStack weapon, ItemStack arrow) {
        return arrow;
    }
}
