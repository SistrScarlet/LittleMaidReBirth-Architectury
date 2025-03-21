package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.Instrument;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.sistr.littlemaidrebirth.block.SalaryBoxBlock;
import net.sistr.littlemaidrebirth.block.SalaryBoxBlockEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidScreenHandler;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import net.sistr.littlemaidrebirth.item.LittleMaidSpawnEggItem;

import static net.sistr.littlemaidrebirth.LMRBMod.MODID;

public class Registration {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(MODID, RegistryKeys.ENTITY_TYPE);
    private static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(MODID, RegistryKeys.ITEM_GROUP);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MODID, RegistryKeys.ITEM);
    private static final DeferredRegister<ScreenHandlerType<?>> SCREEN_HANDLERS = DeferredRegister.create(MODID, RegistryKeys.SCREEN_HANDLER);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MODID, RegistryKeys.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MODID, RegistryKeys.BLOCK_ENTITY_TYPE);

    public static void init() {
        ENTITIES.register();
        ITEM_GROUPS.register();
        BLOCKS.register();
        ITEMS.register();
        SCREEN_HANDLERS.register();
        BLOCK_ENTITIES.register();
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

    //アイテムグループ
    public static final RegistrySupplier<ItemGroup> ITEM_GROUP = ITEM_GROUPS.register("common",
            () -> CreativeTabRegistry.create(Text.translatable("itemGroup.littlemaidrebirth.common"),
                    Items.CAKE::getDefaultStack));

    //ブロック
    public static final RegistrySupplier<SalaryBoxBlock> SALARY_BOX_BLOCK =
            BLOCKS.register("salary_box", () -> new SalaryBoxBlock(
                    AbstractBlock.Settings
                            .create()
                            .mapColor(MapColor.OAK_TAN)
                            .instrument(Instrument.BASS)
                            .strength(2.5f)
                            .sounds(BlockSoundGroup.WOOD)
                            .burnable()
            ));

    //アイテム
    public static final RegistrySupplier<Item> LITTLE_MAID_SPAWN_EGG_ITEM =
            ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEggItem::new);
    /*public static final RegistrySupplier<Item> IFF_COPY_BOOK_ITEM =
            ITEMS.register("iff_copy_book", IFFCopyBookItem::new);*/

    //ブロックアイテム
    public static final RegistrySupplier<Item> SALARY_BOX_BLOCK_ITEM =
            ITEMS.register("salary_box", () -> new BlockItem(SALARY_BOX_BLOCK.get(), new Item.Settings()
                    .arch$tab(ITEM_GROUP)));

    //スクリーンハンドラ
    public static final RegistrySupplier<ScreenHandlerType<LittleMaidScreenHandler>> LITTLE_MAID_SCREEN_HANDLER =
            SCREEN_HANDLERS.register("little_maid", () -> MenuRegistry.ofExtended(LittleMaidScreenHandler::new));

    //ブロックエンティティ
    public static final RegistrySupplier<BlockEntityType<SalaryBoxBlockEntity>> SALARY_BOX_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("salary_box", () ->
                    BlockEntityType.Builder.create(SalaryBoxBlockEntity::new, SALARY_BOX_BLOCK.get())
                            .build(Util.getChoiceType(TypeReferences.BLOCK_ENTITY, "salary_box")));
}
