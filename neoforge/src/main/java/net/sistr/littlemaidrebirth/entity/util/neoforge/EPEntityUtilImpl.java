package net.sistr.littlemaidrebirth.entity.util.neoforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

public class EPEntityUtilImpl {
    public static PersistentProjectileEntity arrowCustomHook(
            BowItem bowItem, PersistentProjectileEntity projectile) {
        // NeoForge 1.21.1 で IItemExtension.customArrow が廃止されたため、そのまま返す
        return projectile;
    }

    public static ItemStack arrowCustomHook(LivingEntity user, ItemStack weapon, ItemStack arrow) {
        return CommonHooks.getProjectile(user, weapon, arrow);
    }
}
