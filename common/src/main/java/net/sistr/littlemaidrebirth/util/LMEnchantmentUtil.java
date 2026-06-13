package net.sistr.littlemaidrebirth.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

/** 1.21 のデータ駆動エンチャント対応: RegistryKey から RegistryEntry を引くヘルパー。 */
public final class LMEnchantmentUtil {
    private LMEnchantmentUtil() {}

    public static RegistryEntry<Enchantment> entry(World world, RegistryKey<Enchantment> key) {
        return world.getRegistryManager()
                .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(key);
    }
}
