package net.sistr.littlemaidrebirth.item;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;
import org.jetbrains.annotations.Nullable;

public class LittleMaidSpawnEggItem extends ArchitecturySpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(Registration.LITTLE_MAID_MOB, 0xFFFFFF, 0x804000,
                new Item.Settings().arch$tab(ModSetup.ITEM_GROUP));
    }
}
