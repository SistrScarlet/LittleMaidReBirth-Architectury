package net.sistr.littlemaidrebirth.setup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.item.IFFCopyBookItem;
import net.sistr.littlemaidrebirth.item.LittleMaidSpawnEggItem;

import static net.sistr.littlemaidrebirth.LittleMaidReBirthMod.MODID;

public class Registration implements ModInitializer {

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "little_maid_spawn_egg"), LITTLE_MAID_SPAWN_EGG_ITEM);
        Registry.register(Registry.ITEM, new Identifier(MODID, "iff_copy_book"), IFF_COPY_BOOK_ITEM);
    }

    //エンティティ
    public static final EntityType<LittleMaidEntity> LITTLE_MAID_MOB =
            Registry.register(Registry.ENTITY_TYPE, new Identifier(MODID, "little_maid_mob"),
                    FabricEntityTypeBuilder.<LittleMaidEntity>create(SpawnGroup.CREATURE, LittleMaidEntity::new)
                            .dimensions(EntityDimensions.changing(0.5F, 1.35F)).build());

    //アイテム
    //スポーンエッグはそのスポーンエッグのエンティティの後に初期化すること
    public static final Item LITTLE_MAID_SPAWN_EGG_ITEM = new LittleMaidSpawnEggItem();
    public static final Item IFF_COPY_BOOK_ITEM = new IFFCopyBookItem();

    //スクリーンハンドラー
    public static final ScreenHandlerType<LittleMaidScreenHandler> LITTLE_MAID_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerExtended(new Identifier(MODID, "little_maid"), LittleMaidScreenHandler::new);
}
