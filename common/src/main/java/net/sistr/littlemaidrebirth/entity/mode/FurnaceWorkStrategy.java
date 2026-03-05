package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.util.AbstractFurnaceAccessor;

public class FurnaceWorkStrategy implements WorkStrategy<AbstractFurnaceBlockEntity> {

  @Override
  public Optional<AbstractFurnaceBlockEntity> getBlockEntity(World world, BlockPos pos) {
    BlockEntity be = world.getBlockEntity(pos);
    if (be instanceof AbstractFurnaceBlockEntity furnace) {
      return Optional.of(furnace);
    }
    return Optional.empty();
  }

  @Override
  public boolean hasRequiredItems(Inventory inventory) {
    return getFuelSlot(inventory).isPresent();
  }

  @Override
  public boolean isUsableTarget(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, World world) {
    return furnace.isEmpty() && canCookInFurnace(furnace, inventory, world);
  }

  @Override
  public boolean hasRemainingWork(AbstractFurnaceBlockEntity furnace) {
    return !furnace.isEmpty();
  }

  @Override
  public boolean shouldContinueWork(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, World world) {
    // 結果スロットにアイテムがあればtrue
    ItemStack result = furnace.getStack(2);
    if (!result.isEmpty()) {
      return true;
    }

    boolean burning = ((AbstractFurnaceAccessor) furnace).isBurningFire_LM();
    if (burning) {
      for (int slot : furnace.getAvailableSlots(Direction.UP)) {
        if (!furnace.getStack(slot).isEmpty()) {
          return true;
        }
      }
    }

    var recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();
    return (burning || getFuelSlot(inventory).isPresent())
        && getAnyCookableItem(inventory, world, recipeType, i -> true).isPresent();
  }

  @Override
  public void doWork(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, World world, WorkActions actions) {
    var recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();

    // 焼けるものがあれば突っ込む
    getCookableSlot(inventory, world, recipeType)
        .ifPresent(cookableIdx -> tryInsertCookable(furnace, inventory, cookableIdx, actions));
    // 燃料があれば突っ込む
    getFuelSlot(inventory)
        .ifPresent(fuelIdx -> tryInsertFuel(furnace, inventory, fuelIdx, actions));
    // 焼けてたら取り出す
    tryExtractResult(furnace, inventory, actions);
  }

  @Override
  public void extractAll(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, WorkActions actions) {
    for (int i = 0; i < furnace.size(); i++) {
      var stack = furnace.getStack(i);
      if (!stack.isEmpty()) {
        stack = HopperBlockEntity.transfer(null, inventory, stack, null);
        if (stack.isEmpty()) {
          furnace.removeStack(i);
        } else {
          furnace.setStack(i, stack);
        }
      }
    }
  }

  @Override
  public String blockPosNbtKey() {
    return "FurnacePos";
  }

  private boolean canCookInFurnace(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, World world) {
    var recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();
    for (int slot : furnace.getAvailableSlots(Direction.UP)) {
      ItemStack stack = furnace.getStack(slot);
      if (!stack.isEmpty()) {
        continue;
      }
      if (getAnyCookableItem(
              inventory,
              world,
              recipeType,
              cookable -> furnace.canInsert(slot, cookable, Direction.UP))
          .isPresent()) {
        return true;
      }
    }
    return false;
  }

  private OptionalInt getFuelSlot(Inventory inventory) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (AbstractFurnaceBlockEntity.canUseAsFuel(stack)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  private Optional<ItemStack> getAnyCookableItem(
      Inventory inventory,
      World world,
      RecipeType<? extends AbstractCookingRecipe> recipeType,
      Predicate<ItemStack> predicate) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty()
          && getRecipe(world, stack, recipeType).isPresent()
          && predicate.test(stack)) {
        return Optional.of(stack);
      }
    }
    return Optional.empty();
  }

  private OptionalInt getCookableSlot(
      Inventory inventory, World world, RecipeType<? extends AbstractCookingRecipe> recipeType) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (getRecipe(world, stack, recipeType).isPresent()) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  private Optional<? extends AbstractCookingRecipe> getRecipe(
      World world, ItemStack stack, RecipeType<? extends AbstractCookingRecipe> recipeType) {
    return world.getRecipeManager().getFirstMatch(recipeType, new SimpleInventory(stack), world);
  }

  private void tryInsertCookable(
      AbstractFurnaceBlockEntity furnace,
      Inventory inventory,
      int cookableIndex,
      WorkActions actions) {
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
      actions.pickupAction();
      break;
    }
  }

  private void tryInsertFuel(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, int fuelIndex, WorkActions actions) {
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
      actions.pickupAction();
      actions.playSoundIfReady(LMSounds.ADD_FUEL);
      break;
    }
  }

  private void tryExtractResult(
      AbstractFurnaceBlockEntity furnace, Inventory inventory, WorkActions actions) {
    int[] resultSlots = furnace.getAvailableSlots(Direction.DOWN);
    for (int resultSlot : resultSlots) {
      ItemStack resultStack = furnace.getStack(resultSlot);
      if (resultStack.isEmpty()) {
        continue;
      }
      if (!furnace.canExtract(resultSlot, resultStack, Direction.DOWN)) {
        continue;
      }
      actions.pickupAction();
      actions.playSoundIfReady(LMSounds.COOKING_OVER);
      ItemStack copy = resultStack.copy();
      ItemStack leftover =
          HopperBlockEntity.transfer(furnace, inventory, furnace.removeStack(resultSlot, 1), null);
      if (leftover.isEmpty()) {
        furnace.markDirty();
        continue;
      }
      furnace.setStack(resultSlot, copy);
    }
  }
}
