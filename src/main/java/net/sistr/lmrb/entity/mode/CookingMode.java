package net.sistr.lmrb.entity.mode;

import com.google.common.collect.Sets;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.lmrb.api.mode.Mode;
import net.sistr.lmrb.api.mode.ModeManager;
import net.sistr.lmrb.entity.InventorySupplier;
import net.sistr.lmrb.util.AbstractFurnaceAccessor;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

public class CookingMode<T extends PathAwareEntity & InventorySupplier> implements Mode {
    private final T mob;
    private final int inventoryStart;
    private final int inventoryEnd;
    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;

    public CookingMode(T mob, int inventoryStart, int inventoryEnd) {
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        if (canUseFurnace()) {
            return true;
        }
        if (--findCool < 0) {
            findCool = 60;
            if (getFuel().isPresent()) {
                furnacePos = findFurnacePos().orElse(null);
                return furnacePos != null;
            }
        }
        return false;
    }

    public boolean canUseFurnace() {
        //かまどがなければfalse
        if (furnacePos == null) {
            return false;
        }
        AbstractFurnaceBlockEntity furnace = getFurnace(furnacePos).orElse(null);
        if (furnace == null) {
            return false;
        }
        //結果スロットが埋まってる場合はtrue
        for (int availableSlot : furnace.getAvailableSlots(Direction.DOWN)) {
            ItemStack result = furnace.getStack(availableSlot);
            if (!result.isEmpty() && furnace.canExtract(availableSlot, result, Direction.DOWN)) {
                return true;
            }
        }
        //何か焼いている場合はtrue
        if (((AbstractFurnaceAccessor) furnace).isBurningFire_LM()) {
            for (int availableSlot : furnace.getAvailableSlots(Direction.UP)) {
                if (!furnace.getStack(availableSlot).isEmpty()) {
                    return true;
                }
            }
        }
        //焼くものがあり、燃料もある場合はtrue
        //待つ必要が無く、焼きたいわけでもない場合はfalse
        return getAllCoockable(((AbstractFurnaceAccessor) furnace).getRecipeType_LM()).findAny().isPresent();
    }

    public Optional<AbstractFurnaceBlockEntity> getFurnace(BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        BlockEntity tile = mob.world.getBlockEntity(pos);
        if (tile instanceof AbstractFurnaceBlockEntity) {
            return Optional.of((AbstractFurnaceBlockEntity) tile);
        }
        return Optional.empty();
    }

    public Stream<ItemStack> getAllCoockable(RecipeType<? extends AbstractCookingRecipe> recipeType) {
        Inventory inventory = this.mob.getInventory();
        Stream.Builder<ItemStack> builder = Stream.builder();
        for (int i = 0; i < inventory.size(); ++i) {
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
        BlockPos ownerPos = mob.getBlockPos();
        //垂直方向に5ブロック調査
        for (int l = 0; l < 5; l++) {
            Optional<BlockPos> optional = findLayer(l, ownerPos);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> findLayer(int layer, BlockPos basePos) {
        BlockPos center;
        //原点高さ、一個上、一個下、二個上、二個下の順にcenterをズラす
        if (layer % 2 == 0) {
            center = basePos.down(MathHelper.floor(layer / 2F + 0.5F));
        } else {
            center = basePos.up(MathHelper.floor(layer / 2F + 0.5F));
        }
        Set<BlockPos> prevSearched = Sets.newHashSet(center);
        Set<BlockPos> allSearched = Sets.newHashSet();
        //水平方向に16ブロック調査
        for (int k = 0; k < 16; k++) {
            Optional<BlockPos> optional = findHorizon(prevSearched, allSearched);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> findHorizon(Set<BlockPos> prevSearched, Set<BlockPos> allSearched) {
        Set<BlockPos> nowSearched = Sets.newHashSet();
        //前回調査地点を起点にする
        for (BlockPos pos : prevSearched) {
            //起点に隣接する水平四ブロックを調査
            for (int i = 0; i < 4; i++) {
                Direction d = Direction.fromHorizontal(i);
                BlockPos checkPos = pos.offset(d);
                //既に調査済みの地点は除外
                if (allSearched.contains(checkPos)) {
                    continue;
                }
                //使用不能なかまどは除外し、次回検索時の起点に加える
                if (!canUseFurnace(getFurnace(checkPos).orElse(null))) {
                    nowSearched.add(checkPos);
                    continue;
                }
                //六面埋まったブロックは除外し、これを起点とした調査も打ち切る
                if (!isTouchAir(mob.world, checkPos)) {
                    allSearched.add(checkPos);
                    nowSearched.remove(checkPos);
                    continue;
                }
                //除外されなければ値を返す
                return Optional.of(checkPos);
            }
        }
        //次回調査用
        allSearched.addAll(nowSearched);
        prevSearched.clear();
        prevSearched.addAll(nowSearched);
        return Optional.empty();
    }

    public boolean canUseFurnace(AbstractFurnaceBlockEntity tile) {
        if (tile == null) {
            return false;
        }
        for (int slot : tile.getAvailableSlots(Direction.UP)) {
            ItemStack stack = tile.getStack(slot);
            if (!stack.isEmpty()) continue;
            RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) tile).getRecipeType_LM();
            if (getAllCoockable(recipeType)
                    .anyMatch(cookable -> tile.canInsert(slot, cookable, Direction.UP))) {
                return true;
            }
        }
        return false;
    }

    public boolean isTouchAir(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (isAir(world, pos, dir)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAir(World world, BlockPos pos, Direction dir) {
        return world.isAir(pos.offset(dir));
    }

    @Override
    public void startExecuting() {
        findCool = 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return canUseFurnace();
    }

    @Override
    public void tick() {
        AbstractFurnaceBlockEntity furnace = getFurnace(furnacePos)
                .orElse(getFurnace(findFurnacePos().orElse(null))
                        .orElse(null));
        if (furnace == null) {
            furnacePos = null;
            return;
        }

        this.mob.getLookControl().lookAt(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

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

        if (!this.mob.isSneaking()) {
            this.mob.setSneaking(true);
        }

        Inventory inventory = this.mob.getInventory();

        RecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();

        getCookable(recipeType).ifPresent(cookableIndex -> tryInsertCookable(furnace, inventory, cookableIndex));
        getFuel().ifPresent(fuelIndex -> tryInsertFuel(furnace, inventory, fuelIndex));
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
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable) mob).play(LMSounds.COOKING_START);
            }
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
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable) mob).play(LMSounds.ADD_FUEL);
            }
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
            this.mob.swingHand(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
            if (mob instanceof SoundPlayable) {
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

    @Override
    public void resetTask() {
        this.mob.setSneaking(false);
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundTag tag) {
        if (furnacePos != null)
            tag.put("FurnacePos", NbtHelper.fromBlockPos(furnacePos));
    }

    @Override
    public void readModeData(CompoundTag tag) {
        if (tag.contains("FurnacePos"))
            furnacePos = NbtHelper.toBlockPos(tag.getCompound("FurnacePos"));
    }

    @Override
    public String getName() {
        return "Cooking";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(Items.BOWL);
        ModeManager.INSTANCE.register(CookingMode.class, items);
    }
}
