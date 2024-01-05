package net.sistr.littlemaidrebirth.item;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;
import org.jetbrains.annotations.Nullable;

public class LittleMaidSpawnEggItem extends SpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(null, 0xFFFFFF, 0x804000,
                new Item.Settings().group(ModSetup.ITEM_GROUP));
    }

    @Override
    public EntityType<?> getEntityType(@Nullable NbtCompound nbt) {
        return Registration.LITTLE_MAID_MOB.get();
    }
}
