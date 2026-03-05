package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.util.BrewingStandAccessor;

public class BrewingWorkStrategy implements WorkStrategy<BrewingStandBlockEntity> {
  private static final int INGREDIENT_SLOT = 3;
  private static final int FUEL_SLOT = 4;
  private static final int POTION_SLOT_START = 0;
  private static final int POTION_SLOT_END = 2;

  @Override
  public Optional<BrewingStandBlockEntity> getBlockEntity(World world, BlockPos pos) {
    BlockEntity be = world.getBlockEntity(pos);
    if (be instanceof BrewingStandBlockEntity stand) {
      return Optional.of(stand);
    }
    return Optional.empty();
  }

  @Override
  public boolean hasRequiredItems(Inventory inventory) {
    return getIngredientSlot(inventory).isPresent() && getPotionBottleSlot(inventory).isPresent();
  }

  @Override
  public boolean isUsableTarget(BrewingStandBlockEntity stand, Inventory inventory, World world) {
    return isEmpty(stand) && hasBrewableRecipeInInventory(inventory);
  }

  @Override
  public boolean hasRemainingWork(BrewingStandBlockEntity stand) {
    return isBrewing(stand) || !isEmpty(stand);
  }

  @Override
  public boolean shouldContinueWork(
      BrewingStandBlockEntity stand, Inventory inventory, World world) {
    if (isBrewing(stand)) {
      return true;
    }
    // ポーション瓶スロットにアイテムがあればtrue
    for (int i = POTION_SLOT_START; i <= POTION_SLOT_END; i++) {
      if (!stand.getStack(i).isEmpty()) {
        return true;
      }
    }
    // 材料スロットにアイテムが残っていればtrue
    if (!stand.getStack(INGREDIENT_SLOT).isEmpty()) {
      return true;
    }
    return hasBrewableRecipeInInventory(inventory);
  }

  @Override
  public void doWork(
      BrewingStandBlockEntity stand, Inventory inventory, World world, WorkActions actions) {
    // 醸造中は待機
    if (isBrewing(stand)) {
      return;
    }

    boolean hasPotionInStand = hasPotionsInStand(stand);

    if (hasPotionInStand) {
      // 醸造台にポーションがある: 連続醸造または回収
      tryExtractUnusableIngredient(stand, inventory, actions);

      if (stand.getStack(INGREDIENT_SLOT).isEmpty()) {
        OptionalInt nextIngredient = getMatchingIngredient(stand, inventory);
        if (nextIngredient.isPresent()) {
          tryInsertFuel(stand, inventory, actions);
          tryInsertIngredient(stand, inventory, nextIngredient.getAsInt(), actions);
        } else {
          tryExtractAllPotions(stand, inventory, actions);
        }
      }
    } else {
      // 醸造台が空: 新規バッチ開始
      tryExtractIngredientSlot(stand, inventory, actions);
      tryInsertFuel(stand, inventory, actions);
      tryInsertPotionBottles(stand, inventory, actions);

      if (hasPotionsInStand(stand)) {
        OptionalInt matchingIngredient = getMatchingIngredient(stand, inventory);
        matchingIngredient.ifPresent(idx -> tryInsertIngredient(stand, inventory, idx, actions));
      }
    }
  }

  @Override
  public void extractAll(BrewingStandBlockEntity stand, Inventory inventory, WorkActions actions) {
    for (int i = 0; i < stand.size(); i++) {
      tryTransferToInventory(stand, i, inventory, actions);
    }
  }

  @Override
  public String blockPosNbtKey() {
    return "BrewingStandPos";
  }

  private boolean isBrewing(BrewingStandBlockEntity stand) {
    return ((BrewingStandAccessor) stand).getBrewTime_LM() > 0;
  }

  private boolean isEmpty(BrewingStandBlockEntity stand) {
    for (int i = 0; i < stand.size(); i++) {
      if (!stand.getStack(i).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private boolean isPotionItem(ItemStack stack) {
    return stack.isOf(Items.POTION)
        || stack.isOf(Items.SPLASH_POTION)
        || stack.isOf(Items.LINGERING_POTION)
        || stack.isOf(Items.GLASS_BOTTLE);
  }

  private OptionalInt getIngredientSlot(Inventory inventory) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && BrewingRecipeRegistry.isValidIngredient(stack)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  private OptionalInt getPotionBottleSlot(Inventory inventory) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && isPotionItem(stack)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  private boolean hasBrewableRecipeInInventory(Inventory inventory) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack ingredient = inventory.getStack(i);
      if (ingredient.isEmpty() || !BrewingRecipeRegistry.isValidIngredient(ingredient)) {
        continue;
      }
      for (int j = 0; j < inventory.size(); ++j) {
        ItemStack potion = inventory.getStack(j);
        if (!potion.isEmpty()
            && isPotionItem(potion)
            && BrewingRecipeRegistry.hasRecipe(potion, ingredient)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasPotionsInStand(BrewingStandBlockEntity stand) {
    for (int i = POTION_SLOT_START; i <= POTION_SLOT_END; i++) {
      if (!stand.getStack(i).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private OptionalInt getMatchingIngredient(BrewingStandBlockEntity stand, Inventory inventory) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack invStack = inventory.getStack(i);
      if (invStack.isEmpty() || !BrewingRecipeRegistry.isValidIngredient(invStack)) {
        continue;
      }
      for (int slot = POTION_SLOT_START; slot <= POTION_SLOT_END; slot++) {
        ItemStack potionStack = stand.getStack(slot);
        if (!potionStack.isEmpty() && BrewingRecipeRegistry.hasRecipe(potionStack, invStack)) {
          return OptionalInt.of(i);
        }
      }
    }
    return OptionalInt.empty();
  }

  private void tryInsertFuel(
      BrewingStandBlockEntity stand, Inventory inventory, WorkActions actions) {
    if (((BrewingStandAccessor) stand).getFuel_LM() > 0) {
      return;
    }
    if (!stand.getStack(FUEL_SLOT).isEmpty()) {
      return;
    }
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && stack.isOf(Items.BLAZE_POWDER)) {
        ItemStack toInsert = stack.split(1);
        stand.setStack(FUEL_SLOT, toInsert);
        stand.markDirty();
        actions.pickupAction();
        actions.playSoundIfReady(LMSounds.ADD_FUEL);
        return;
      }
    }
  }

  private void tryInsertPotionBottles(
      BrewingStandBlockEntity stand, Inventory inventory, WorkActions actions) {
    for (int slot = POTION_SLOT_START; slot <= POTION_SLOT_END; slot++) {
      if (!stand.getStack(slot).isEmpty()) {
        continue;
      }
      for (int i = 0; i < inventory.size(); ++i) {
        ItemStack potionStack = inventory.getStack(i);
        if (potionStack.isEmpty() || !isPotionItem(potionStack)) {
          continue;
        }
        if (!hasMatchingIngredientFor(potionStack, inventory)) {
          continue;
        }
        ItemStack toInsert = potionStack.split(1);
        stand.setStack(slot, toInsert);
        stand.markDirty();
        actions.pickupAction();
        break;
      }
    }
  }

  private boolean hasMatchingIngredientFor(ItemStack potion, Inventory inventory) {
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty()
          && BrewingRecipeRegistry.isValidIngredient(stack)
          && BrewingRecipeRegistry.hasRecipe(potion, stack)) {
        return true;
      }
    }
    return false;
  }

  private void tryInsertIngredient(
      BrewingStandBlockEntity stand,
      Inventory inventory,
      int ingredientIndex,
      WorkActions actions) {
    if (!stand.getStack(INGREDIENT_SLOT).isEmpty()) {
      return;
    }
    ItemStack ingredientStack = inventory.getStack(ingredientIndex);
    ItemStack toInsert = ingredientStack.split(1);
    stand.setStack(INGREDIENT_SLOT, toInsert);
    stand.markDirty();
    actions.pickupAction();
    actions.playSoundIfReady(LMSounds.COOKING_START);
  }

  private void tryExtractUnusableIngredient(
      BrewingStandBlockEntity stand, Inventory inventory, WorkActions actions) {
    ItemStack ingredient = stand.getStack(INGREDIENT_SLOT);
    if (ingredient.isEmpty()) {
      return;
    }
    if (ingredient.isOf(Items.GLASS_BOTTLE)) {
      tryTransferToInventory(stand, INGREDIENT_SLOT, inventory, actions);
      return;
    }
    for (int slot = POTION_SLOT_START; slot <= POTION_SLOT_END; slot++) {
      ItemStack potionStack = stand.getStack(slot);
      if (!potionStack.isEmpty() && BrewingRecipeRegistry.hasRecipe(potionStack, ingredient)) {
        return;
      }
    }
    tryTransferToInventory(stand, INGREDIENT_SLOT, inventory, actions);
  }

  private void tryExtractAllPotions(
      BrewingStandBlockEntity stand, Inventory inventory, WorkActions actions) {
    for (int slot = POTION_SLOT_START; slot <= POTION_SLOT_END; slot++) {
      if (stand.getStack(slot).isEmpty()) {
        continue;
      }
      if (tryTransferToInventory(stand, slot, inventory, actions)) {
        actions.playSoundIfReady(LMSounds.COOKING_OVER);
      }
    }
  }

  private void tryExtractIngredientSlot(
      BrewingStandBlockEntity stand, Inventory inventory, WorkActions actions) {
    ItemStack ingredientStack = stand.getStack(INGREDIENT_SLOT);
    if (ingredientStack.isEmpty()) {
      return;
    }
    if (ingredientStack.isOf(Items.GLASS_BOTTLE)) {
      tryTransferToInventory(stand, INGREDIENT_SLOT, inventory, actions);
    }
  }

  private boolean tryTransferToInventory(
      BrewingStandBlockEntity stand, int slot, Inventory inventory, WorkActions actions) {
    ItemStack stack = stand.getStack(slot);
    if (stack.isEmpty()) {
      return false;
    }
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack invStack = inventory.getStack(i);
      if (invStack.isEmpty()) {
        inventory.setStack(i, stack.copy());
        stand.removeStack(slot);
        stand.markDirty();
        actions.pickupAction();
        return true;
      }
      if (ItemStack.canCombine(invStack, stack) && invStack.getCount() < invStack.getMaxCount()) {
        int transferable = Math.min(stack.getCount(), invStack.getMaxCount() - invStack.getCount());
        invStack.increment(transferable);
        stack.decrement(transferable);
        if (stack.isEmpty()) {
          stand.removeStack(slot);
          stand.markDirty();
          actions.pickupAction();
          return true;
        }
      }
    }
    return false;
  }
}
