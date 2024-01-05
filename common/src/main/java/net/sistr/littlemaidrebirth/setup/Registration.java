package net.sistr.littlemaidrebirth.setup;

import me.shedaniel.architectury.registry.DeferredRegister;
import me.shedaniel.architectury.registry.MenuRegistry;
import me.shedaniel.architectury.registry.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.registry.Registry;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import net.sistr.littlemaidrebirth.item.LittleMaidSpawnEggItem;

import static net.sistr.littlemaidrebirth.LMRBMod.MODID;

public class Registration {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(MODID, Registry.ENTITY_TYPE_KEY);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, Registry.ITEM_KEY);
    private static final DeferredRegister<ScreenHandlerType<?>> SCREEN_HANDLERS = DeferredRegister.create(MODID, Registry.MENU_KEY);

    public static void init() {
        ENTITIES.register();
        ITEMS.register();
        SCREEN_HANDLERS.register();
    }

    //エンティティ
    public static final RegistrySupplier<EntityType<LittleMaidEntity>> LITTLE_MAID_MOB =
            ENTITIES.register("little_maid_mob", () ->
                    EntityType.Builder.<LittleMaidEntity>create(LittleMaidEntity::new, SpawnGroup.CREATURE)
                            .setDimensions(0.5F, 1.35F).build("little_maid_mob"));
    public static final RegistrySupplier<EntityType<MaidSoulEntity>> MAID_SOUL_ENTITY =
            ENTITIES.register("maid_soul", () ->
                    EntityType.Builder.<MaidSoulEntity>create(MaidSoulEntity::new, SpawnGroup.MISC)
                            .setDimensions(0.5F, 0.5F).build("maid_soul"));

    //アイテム
    public static final RegistrySupplier<Item> LITTLE_MAID_SPAWN_EGG_ITEM =
            ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEggItem::new);
    /*public static final RegistrySupplier<Item> IFF_COPY_BOOK_ITEM =
            ITEMS.register("iff_copy_book", IFFCopyBookItem::new);*/

    //スクリーンハンドラ
    public static final RegistrySupplier<ScreenHandlerType<LittleMaidScreenHandler>> LITTLE_MAID_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("little_maid", () -> MenuRegistry.ofExtended(LittleMaidScreenHandler::new));
}
