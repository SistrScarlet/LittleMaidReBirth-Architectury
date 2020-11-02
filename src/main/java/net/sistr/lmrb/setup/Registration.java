package net.sistr.lmrb.setup;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.sistr.lmrb.entity.LittleMaidEntity;
import net.sistr.lmrb.entity.LittleMaidScreenHandler;
import net.sistr.lmrb.item.LittleMaidSpawnEgg;

import static net.sistr.lmrb.LittleMaidReBirthMod.MODID;

public class Registration {

    public static void init() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "little_maid_spawn_egg"), LITTLE_MAID_SPAWN_EGG_ITEM);
    }

    //エンティティ
    public static final EntityType<LittleMaidEntity> LITTLE_MAID_MOB =
            Registry.register(Registry.ENTITY_TYPE, new Identifier(MODID, "little_maid_mob"),
                    FabricEntityTypeBuilder.<LittleMaidEntity>create(SpawnGroup.CREATURE, LittleMaidEntity::new)
                            .dimensions(EntityDimensions.fixed(0.8F, 1.2F)).build());

    //アイテム
    //スポーンエッグはそのスポーンエッグのエンティティの後に初期化すること
    public static final Item LITTLE_MAID_SPAWN_EGG_ITEM = new LittleMaidSpawnEgg();

    //スクリーンハンドラー
    public static final ScreenHandlerType<LittleMaidScreenHandler> LITTLE_MAID_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerExtended(new Identifier(MODID, "little_maid"), LittleMaidScreenHandler::new);
}
