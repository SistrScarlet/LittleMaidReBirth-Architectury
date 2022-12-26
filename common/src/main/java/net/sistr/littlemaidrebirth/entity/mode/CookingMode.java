package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

public class CookingMode extends Mode {
    private final LittleMaidEntity mob;
    private final int inventoryStart;
    private final int inventoryEnd;
    @Nullable
    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;
    private int playSoundCool;

    public CookingMode(ModeType<? extends CookingMode> modeType, String name, LittleMaidEntity mob, int inventoryStart, int inventoryEnd) {
        super(modeType, name);
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        if (0 < --findCool) {
            return false;
        }
        findCool = 60;
        //燃料がないならリターン
        if (getFuel().isEmpty()) {
            return false;
        }
        //かまどが無いか、使用不可な場合は再探索
        if (furnacePos == null
                || (furnacePos.isWithinDistance(this.mob.getPos(), 6)
                && getFurnaceBlockEntity(furnacePos).isPresent())) {
            furnacePos = findFurnacePos().orElse(null);
            if (furnacePos == null) {
                return false;
            }
        }
        AbstractFurnaceBlockEntity furnace = getFurnaceBlockEntity(furnacePos).orElse(null);
        if (furnace == null) {
            furnacePos = null;
            return false;
        }
        //焼くものがある場合はtrue
        return getAllCoockable(((AbstractFurnaceAccessor) furnace).getRecipeType_LM()).findAny().isPresent();
    }

    public OptionalInt getFuel() {
        Inventory inventory = this.mob.getInventory();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
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
     * ここで言う使用可能なかまどとは、手持ちのアイテムを焼けるかどうかで判定する
     */
    public Optional<BlockPos> findFurnacePos() {
        return BlockFinder.searchTargetBlock(mob.getBlockPos(), this::canUseFurnace, this::canSeeThrough,
                        Arrays.asList(Direction.values()), 1000)
                .filter(pos -> pos.getManhattanDistance(mob.getBlockPos()) < 8);
    }

    public boolean canUseFurnace(BlockPos pos) {
        return getFurnaceBlockEntity(pos).filter(this::canUseFurnace).isPresent();
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

    public boolean canUseFurnace(AbstractFurnaceBlockEntity tile) {
        for (int slot : tile.getAvailableSlots(Direction.UP)) {
            ItemStack stack = tile.getStack(slot);
            if (!stack.isEmpty()) continue;
            //手持ちに焼けるアイテムがあればtrue
            RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) tile).getRecipeType_LM();
            if (getAllCoockable(recipeType)
                    .anyMatch(cookable -> tile.canInsert(slot, cookable, Direction.UP))) {
                return true;
            }
        }
        return false;
    }

    public Stream<ItemStack> getAllCoockable(RecipeType<? extends AbstractCookingRecipe> recipeType) {
        Inventory inventory = this.mob.getInventory();
        Stream.Builder<ItemStack> builder = Stream.builder();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
            ItemStack slotStack = inventory.getStack(i);
            if (getRecipe(slotStack, recipeType).isPresent()) {
                builder.accept(slotStack);
            }
        }
        return builder.build();
    }

    public Optional<? extends AbstractCookingRecipe> getRecipe(ItemStack stack, RecipeType<? extends AbstractCookingRecipe> recipeType) {
        return mob.world.getRecipeManager().getFirstMatch(recipeType, new SimpleInventory(stack), mob.world);
    }

    public boolean canSeeThrough(BlockPos pos) {
        return true;//!mob.world.getBlockState(pos).isSolidBlock(mob.world, pos);
    }

    @Override
    public void startExecuting() {
        findCool = 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        //かまどがなければfalse
        if (furnacePos == null) {
            return false;
        }
        AbstractFurnaceBlockEntity furnace = getFurnaceBlockEntity(furnacePos).orElse(null);
        if (furnace == null) {
            furnacePos = null;
            return false;
        }
        //結果スロットが埋まってる場合はtrue
        //getAvailableSlots(DOWN)では燃料スロットも取ってしまうため、マジックナンバーに頼らざる負えなかった
        ItemStack result = furnace.getStack(2);
        if (!result.isEmpty()) {
            return true;
        }
        //何か焼いている場合はtrue
        if (((AbstractFurnaceAccessor) furnace).isBurningFire_LM()) {
            for (int availableSlot : furnace.getAvailableSlots(Direction.UP)) {
                if (!furnace.getStack(availableSlot).isEmpty()) {
                    return true;
                }
            }
        }
        //燃料と焼くものがある場合はtrue
        //どちらか無ければfalse
        return getFuel().isPresent()
                && getAllCoockable(((AbstractFurnaceAccessor) furnace).getRecipeType_LM()).findAny().isPresent();
    }

    @Override
    public void tick() {
        assert furnacePos != null;
        //shouldContinueExecutingでチェック済みなのでかまどが無い場合はありえない
        AbstractFurnaceBlockEntity furnace = getFurnaceBlockEntity(furnacePos).orElseThrow();

        //視線を向ける
        this.mob.getLookControl().lookAt(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        //かまどの近くに移動
        if (!this.mob.getBlockPos().isWithinDistance(furnacePos, 2)) {
            if (this.mob.isSneaking()) {
                this.mob.setSneaking(false);
            }
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.mob.getNavigation().startMovingTo(furnacePos.getX() + 0.5D, furnacePos.getY() + 0.5D, furnacePos.getZ() + 0.5D, 1);
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
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
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
            playSound();
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
            playSound();
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
            playSound();
            ItemStack copy = resultStack.copy();
            ItemStack leftover = HopperBlockEntity.transfer(furnace, inventory, furnace.removeStack(resultSlot, 1), null);
            if (leftover.isEmpty()) {
                furnace.markDirty();
                continue;
            }

            furnace.setStack(resultSlot, copy);
        }
    }

    public void playSound() {
        if (playSoundCool < 0) {
            playSoundCool = 20;
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
            ((SoundPlayable) mob).play(LMSounds.COOKING_OVER);
        }
    }

    @Override
    public void resetTask() {
        playSoundCool = 0;
        this.mob.setSneaking(false);
        if (furnacePos != null) {
            AbstractFurnaceBlockEntity furnace = getFurnaceBlockEntity(furnacePos).orElse(null);
            if (furnace == null) {
                furnacePos = null;
                return;
            }
            //かまどからアイテムをすべて取り出す
            for (int i = 0; i < furnace.size(); i++) {
                var stack = furnace.getStack(i);
                if (!stack.isEmpty()) {
                    if (((PlayerInventory) this.mob.getInventory()).insertStack(stack)) {
                        furnace.removeStack(i);
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
