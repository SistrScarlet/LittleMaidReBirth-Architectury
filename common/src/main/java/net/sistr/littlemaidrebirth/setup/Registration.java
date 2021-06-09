package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.registry.Registry;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.item.IFFCopyBookItem;
import net.sistr.littlemaidrebirth.item.LittleMaidSpawnEggItem;

import static net.sistr.littlemaidrebirth.LittleMaidReBirthMod.MODID;

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
    public static final EntityType<LittleMaidEntity> LITTLE_MAID_MOB_BEFORE =
            EntityType.Builder.<LittleMaidEntity>create(LittleMaidEntity::new, SpawnGroup.CREATURE)
                    .setDimensions(0.5F, 1.35F).build("little_maid_mob");
    public static final RegistrySupplier<EntityType<LittleMaidEntity>> LITTLE_MAID_MOB =
            ENTITIES.register("little_maid_mob", () -> LITTLE_MAID_MOB_BEFORE);

    //アイテム
    public static final RegistrySupplier<Item> LITTLE_MAID_SPAWN_EGG_ITEM =
            ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEggItem::new);
    public static final Item IFF_COPY_BOOK_ITEM_BEFORE = new IFFCopyBookItem();
    public static final RegistrySupplier<Item> IFF_COPY_BOOK_ITEM =
            ITEMS.register("iff_copy_book", () -> IFF_COPY_BOOK_ITEM_BEFORE);

    //スクリーンハンドラー
    public static final ScreenHandlerType<LittleMaidScreenHandler> LITTLE_MAID_SCREEN_HANDLER_BEFORE =
            MenuRegistry.ofExtended(LittleMaidScreenHandler::new);
    public static final RegistrySupplier<ScreenHandlerType<LittleMaidScreenHandler>> LITTLE_MAID_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("little_maid", () -> LITTLE_MAID_SCREEN_HANDLER_BEFORE);
}
