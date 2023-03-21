package net.sistr.littlemaidrebirth.entity.mode;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.Material;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.AbstractFurnaceAccessor;
import net.sistr.littlemaidrebirth.util.BlockFinder;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public class CookingMode extends Mode {
    //別ディメンションで同一の位置にかまどがある場合はレアケースなので考慮しない
    private static final Object2ObjectOpenHashMap<BlockPos, LittleMaidEntity> USED_FURNACE_MAP = new Object2ObjectOpenHashMap<>();
    private final LittleMaidEntity mob;
    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;
    private int playSoundCool;
    private AbstractFurnaceBlockEntity furnace;

    public CookingMode(ModeType<? extends CookingMode> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name);
        this.mob = mob;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        if (0 < --findCool) {
            return false;
        }
        findCool = 20;
        AbstractFurnaceBlockEntity prev;
        //モードが中断されたあと、再開するときの判定
        //注視しているかまどがあり、使用可能
        if (furnacePos != null && furnacePos.isWithinDistance(this.mob.getPos(), 6)
                && (prev = getFurnaceBlockEntity(furnacePos).orElse(null)) != null
                && !isUsingFurnaceByOtherMaid(furnacePos)) {
            //アイテムが残っている場合はtrue
            if (!prev.isEmpty()) {
                furnace = prev;
                return true;
            }
        } else {
            //かまどは使用不可のためリセット
            furnacePos = null;
        }

        //物を焼き始めるときの判定

        //燃料がないならリターン
        if (getFuel().isEmpty()) {
            return false;
        }
        var recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();
        //かまどが無いか、焼けない場合は再探索
        //なお上でチェックしているため、furnacePosがあるならかまどは必ず使用可能
        if (furnacePos == null
                || !canCookingFurnace(furnace = getFurnaceBlockEntity(furnacePos).orElseThrow())) {
            furnacePos = findFurnacePos().orElse(null);
            if (furnacePos == null) {
                return false;
            }
            furnace = getFurnaceBlockEntity(furnacePos).orElseThrow();
            return true;
        }
        return true;
    }

    public OptionalInt getFuel() {
        Inventory inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack itemstack = inventory.getStack(i);
            if (isFuel(itemstack)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.canUseAsFuel(stack);
    }

    /**
     * 使用可能なかまどを探索する。
     */
    public Optional<BlockPos> findFurnacePos() {
        return BlockFinder.searchTargetBlock(new BlockPos(mob.getEyePos()), this::isTargetFurnace, this::isSearchable,
                Arrays.asList(Direction.values()), 128);
    }

    public boolean isTargetFurnace(BlockPos pos) {
        //他のメイドさんが使ってるかまどはダメ
        if (isUsingFurnaceByOtherMaid(pos)) {
            return false;
        }
        return getFurnaceBlockEntity(pos)
                .filter(AbstractFurnaceBlockEntity::isEmpty)//空のかまど
                .filter(this::canCookingFurnace)//手持ちのアイテムを焼けるかまど
                .isPresent();
    }

    public Optional<AbstractFurnaceBlockEntity> getFurnaceBlockEntity(BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        BlockEntity tile = mob.world.getBlockEntity(pos);
        if (tile instanceof AbstractFurnaceBlockEntity) {
            return Optional.of((AbstractFurnaceBlockEntity) tile);
        }
        return Optional.empty();
    }

    //手持ちのアイテムを焼けるかまどかどうか
    public boolean canCookingFurnace(AbstractFurnaceBlockEntity tile) {
        for (int slot : tile.getAvailableSlots(Direction.UP)) {
            ItemStack stack = tile.getStack(slot);
            if (!stack.isEmpty()) continue;
            //手持ちに焼けるアイテムがあればtrue
            RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) tile).getRecipeType_LM();
            if (getAnyCookableItem(recipeType,
                    cookable -> tile.canInsert(slot, cookable, Direction.UP))
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUsingFurnaceByOtherMaid(BlockPos furnacePos) {
        var user = USED_FURNACE_MAP.get(furnacePos);
        if (user != null && user != this.mob) {
            if (!user.isAlive() || user != user.world.getEntityById(user.getId())) {
                USED_FURNACE_MAP.remove(furnacePos);
                return false;
            }
            return true;
        }
        return false;
    }

    //インベントリからこのレシピタイプで焼けるアイテムを取得
    public Optional<ItemStack> getAnyCookableItem(RecipeType<? extends AbstractCookingRecipe> recipeType,
                                                  Predicate<ItemStack> predicate) {
        Inventory inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack slotStack = inventory.getStack(i);
            if (!slotStack.isEmpty()
                    && getRecipe(slotStack, recipeType).isPresent()
                    && predicate.test(slotStack)) {
                return Optional.of(slotStack);
            }
        }
        return Optional.empty();
    }

    public Optional<? extends AbstractCookingRecipe> getRecipe(ItemStack stack, RecipeType<? extends AbstractCookingRecipe> recipeType) {
        return mob.world.getRecipeManager().getFirstMatch(recipeType, new SimpleInventory(stack), mob.world);
    }

    public boolean isSearchable(BlockPos pos) {
        BlockState state;
        return Math.abs(pos.getY() - this.mob.getY()) < 2
                && pos.isWithinDistance(this.mob.getPos(), 6)
                && ((state = this.mob.world.getBlockState(pos))
                .canPathfindThrough(this.mob.world, pos, NavigationType.LAND)
                //ドアも通過
                || (state.getBlock() instanceof DoorBlock
                && state.getMaterial() != Material.METAL));
    }

    @Override
    public void startExecuting() {
        findCool = 0;
        USED_FURNACE_MAP.put(furnacePos, mob);
        ((SoundPlayable) mob).play(LMSounds.COOKING_START);
    }

    @Override
    public boolean shouldContinueExecuting() {
        //かまどがなければfalse
        if (furnacePos == null) {
            return false;
        }
        //かまどが変わっていたら終了
        var tmp = getFurnaceBlockEntity(furnacePos).orElse(null);
        if (tmp != furnace) {
            furnacePos = null;
            furnace = null;
            return false;
        }
        //結果スロットが埋まってる場合はtrue
        //getAvailableSlots(DOWN)では燃料スロットも取ってしまうため、マジックナンバーに頼らざる負えなかった
        ItemStack result = furnace.getStack(2);
        if (!result.isEmpty()) {
            return true;
        }
        //何か焼いている場合はtrue
        boolean burning = ((AbstractFurnaceAccessor) furnace).isBurningFire_LM();
        if (burning) {
            for (int availableSlot : furnace.getAvailableSlots(Direction.UP)) {
                if (!furnace.getStack(availableSlot).isEmpty()) {
                    return true;
                }
            }
        }
        var recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();
        //燃料と焼くものがある場合はtrue
        //どちらか無ければfalse
        return (burning || getFuel().isPresent())
                && getAnyCookableItem(recipeType, i -> true).isPresent();
    }

    @Override
    public void tick() {
        //視線を向ける
        this.mob.getLookControl().lookAt(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        //かまどの近くに移動
        if (!this.mob.getBlockPos().isWithinDistance(furnacePos, 2.25)) {
            if (this.mob.isSneaking()) {
                this.mob.setSneaking(false);
            }
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                double x = furnacePos.getX() + 0.5D;
                double y = furnacePos.getY() + 0.5D;
                double z = furnacePos.getZ() + 0.5D;
                var path = this.mob.getNavigation().findPathTo(x, y, z, 2);
                this.mob.getNavigation().startMovingAlong(path, 1);
            }
            return;
        }
        this.mob.getNavigation().stop();

        //しゃがむ
        if (!this.mob.isSneaking()) {
            this.mob.setSneaking(true);
        }

        Inventory inventory = this.mob.getInventory();

        RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();

        playSoundCool--;

        //焼けるものがあれば突っ込む
        getCookable(recipeType).ifPresent(cookableIndex -> tryInsertCookable(furnace, inventory, cookableIndex));
        //燃料があれば突っ込む
        getFuel().ifPresent(fuelIndex -> tryInsertFuel(furnace, inventory, fuelIndex));
        //焼けてたら取り出す
        tryExtractItem(furnace, inventory);
    }

    public OptionalInt getCookable(RecipeType<? extends AbstractCookingRecipe> recipeType) {
        Inventory inventory = this.mob.getInventory();
        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack slotStack = inventory.getStack(i);
            if (getRecipe(slotStack, recipeType).isPresent()) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private void tryInsertCookable(AbstractFurnaceBlockEntity furnace, Inventory inventory, int cookableIndex) {
        int[] materialSlots = furnace.getAvailableSlots(Direction.UP);
        for (int materialSlot : materialSlots) {
            ItemStack materialSlotStack = furnace.getStack(materialSlot);
            if (!materialSlotStack.isEmpty()) {
                continue;
            }
            ItemStack material = inventory.getStack(cookableIndex);
            if (!furnace.canInsert(materialSlot, material, Direction.UP)) {
                continue;
            }
            furnace.setStack(materialSlot, material);
            inventory.removeStack(cookableIndex);
            furnace.markDirty();
            pickupAction();
            break;
        }
    }

    private void tryInsertFuel(AbstractFurnaceBlockEntity furnace, Inventory inventory, int fuelIndex) {
        int[] fuelSlots = furnace.getAvailableSlots(Direction.NORTH);
        for (int fuelSlot : fuelSlots) {
            ItemStack fuelSlotStack = furnace.getStack(fuelSlot);
            if (!fuelSlotStack.isEmpty()) {
                continue;
            }
            ItemStack fuel = inventory.getStack(fuelIndex);
            if (!furnace.canInsert(fuelSlot, fuel, Direction.NORTH)) {
                continue;
            }
            furnace.setStack(fuelSlot, fuel);
            inventory.removeStack(fuelIndex);
            furnace.markDirty();
            pickupAction();
            break;
        }
    }

    private void tryExtractItem(AbstractFurnaceBlockEntity furnace, Inventory inventory) {
        int[] resultSlots = furnace.getAvailableSlots(Direction.DOWN);
        for (int resultSlot : resultSlots) {
            ItemStack resultStack = furnace.getStack(resultSlot);
            if (resultStack.isEmpty()) {
                continue;
            }
            if (!furnace.canExtract(resultSlot, resultStack, Direction.DOWN)) {
                continue;
            }
            pickupAction();
            if (playSoundCool < 0) {
                playSoundCool = 20;
                ((SoundPlayable) mob).play(LMSounds.COOKING_OVER);
            }
            ItemStack copy = resultStack.copy();
            ItemStack leftover = HopperBlockEntity.transfer(furnace, inventory, furnace.removeStack(resultSlot, 1), null);
            if (leftover.isEmpty()) {
                furnace.markDirty();
                continue;
            }

            furnace.setStack(resultSlot, copy);
        }
    }

    public void pickupAction() {
        if (playSoundCool < 0) {
            playSoundCool = 20;
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
        }
    }

    @Override
    public void resetTask() {
        playSoundCool = 0;
        this.mob.setSneaking(false);
        if (furnacePos != null) {
            USED_FURNACE_MAP.remove(furnacePos, mob);
            AbstractFurnaceBlockEntity furnace = getFurnaceBlockEntity(furnacePos).orElse(null);
            if (furnace == null) {
                furnacePos = null;
                return;
            }
            //かまどからアイテムをすべて取り出す
            for (int i = 0; i < furnace.size(); i++) {
                var stack = furnace.getStack(i);
                if (!stack.isEmpty()) {
                    stack = HopperBlockEntity.transfer(null, this.mob.getInventory(), stack, null);
                    if (stack.isEmpty()) {
                        furnace.removeStack(i);
                    } else {
                        furnace.setStack(i, stack);
                    }
                }
            }
        }
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(NbtCompound nbt) {
        if (furnacePos != null)
            nbt.put("FurnacePos", NbtHelper.fromBlockPos(furnacePos));
    }

    @Override
    public void readModeData(NbtCompound nbt) {
        if (nbt.contains("FurnacePos"))
            furnacePos = NbtHelper.toBlockPos(nbt.getCompound("FurnacePos"));
    }

}
