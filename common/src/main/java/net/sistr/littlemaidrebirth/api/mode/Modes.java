package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.entity.mode.*;
import net.sistr.littlemaidrebirth.tags.LMTags;

import static net.sistr.littlemaidrebirth.LittleMaidReBirthMod.MODID;

public class Modes {
    public static final ModeType<FencerMode> FENCER_MODE_TYPE;
    public static final ModeType<ArcherMode> ARCHER_MODE_TYPE;
    public static final ModeType<CookingMode> COOKING_MODE_TYPE;
    public static final ModeType<RipperMode> RIPPER_MODE_TYPE;
    public static final ModeType<TorcherMode> TORCHER_MODE_TYPE;
    public static final ModeType<HealerMode> HEALER_MODE_TYPE;

    static {
        FENCER_MODE_TYPE = buildFencerMode().build();
        ARCHER_MODE_TYPE = buildArcherMode().build();
        COOKING_MODE_TYPE = buildCookingMode().build();
        RIPPER_MODE_TYPE = buildRipperMode().build();
        TORCHER_MODE_TYPE = buildTorcherMode().build();
        HEALER_MODE_TYPE = buildHealerMode().build();
    }

    public static ModeType.Builder<FencerMode> buildFencerMode() {
        return ModeType.<FencerMode>builder((type, maid) ->
                new FencerMode(type, "Fencer", maid, 1D, true))
                .addItemMatcher(ItemMatchers.clazz(SwordItem.class))
                .addItemMatcher(ItemMatchers.clazz(AxeItem.class))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.FENCER_MODE));
    }

    public static ModeType.Builder<ArcherMode> buildArcherMode() {
        return ModeType.<ArcherMode>builder((type, maid) ->
                new ArcherMode(type, "Archer", maid, 15F, entity ->
                        entity instanceof LivingEntity && maid.isFriend((LivingEntity) entity)))
                .addItemMatcher(ItemMatchers.clazz(IRangedWeapon.class))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.ARCHER_MODE));
    }

    public static ModeType.Builder<CookingMode> buildCookingMode() {
        return ModeType.<CookingMode>builder((type, maid) ->
                new CookingMode(type, "Cooking", maid, 1, 1 + 18))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.COOKING_MODE));
    }

    public static ModeType.Builder<RipperMode> buildRipperMode() {
        return ModeType.<RipperMode>builder((type, maid) ->
                new RipperMode(type, "Ripper", maid, 8F))
                .addItemMatcher(ItemMatchers.clazz(ShearsItem.class))
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.RIPPER_MODE));
    }

    public static ModeType.Builder<TorcherMode> buildTorcherMode() {
        return ModeType.<TorcherMode>builder((type, maid) ->
                new TorcherMode(type, "Torcher", maid, 8F))
                .addItemMatcher(stack ->
                        stack.getItem() instanceof BlockItem
                                && 9 < ((BlockItem) stack.getItem()).getBlock().getDefaultState().getLuminance())
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.TORCHER_MODE));
    }

    public static ModeType.Builder<HealerMode> buildHealerMode() {
        return ModeType.<HealerMode>builder((type, maid) ->
                new HealerMode(type, "Healer", maid, 0, 1 + 18))
                .addItemMatcher(stack -> stack.getItem().isFood())
                .addItemMatcher(ItemMatchers.tag(LMTags.Items.HEALER_MODE));
    }

    public static void init() {
        register("fencer", FENCER_MODE_TYPE);
        register("archer", ARCHER_MODE_TYPE);
        register("cooking", COOKING_MODE_TYPE);
        register("ripper", RIPPER_MODE_TYPE);
        register("torcher", TORCHER_MODE_TYPE);
        register("healer", HEALER_MODE_TYPE);
    }

    private static void register(String id, ModeType<?> modeType) {
        ModeManager.INSTANCE.register(new Identifier(MODID, id), modeType);
    }

}
