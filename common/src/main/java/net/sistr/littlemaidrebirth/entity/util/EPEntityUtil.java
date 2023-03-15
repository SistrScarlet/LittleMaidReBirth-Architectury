package net.sistr.littlemaidrebirth.entity.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

public class EPEntityUtil {

    @ExpectPlatform
    public static PersistentProjectileEntity arrowCustomHook(BowItem bowItem, PersistentProjectileEntity arrow) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ItemStack arrowCustomHook(LivingEntity user, ItemStack weapon, ItemStack arrow) {
        throw new AssertionError();
    }

}
