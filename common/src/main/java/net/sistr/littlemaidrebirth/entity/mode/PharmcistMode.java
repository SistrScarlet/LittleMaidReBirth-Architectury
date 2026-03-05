package net.sistr.littlemaidrebirth.entity.mode;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.BlockFinder;
import net.sistr.littlemaidrebirth.util.BrewingStandAccessor;
import org.jetbrains.annotations.Nullable;

public class PharmcistMode extends Mode {
  private static final Object2ObjectOpenHashMap<BlockPos, LittleMaidEntity> USED_BREWING_STAND_MAP =
      new Object2ObjectOpenHashMap<>();
  private static final int INGREDIENT_SLOT = 3;
  private static final int FUEL_SLOT = 4;
  private static final int POTION_SLOT_START = 0;
  private static final int POTION_SLOT_END = 2;

  private final LittleMaidEntity mob;
  @Nullable private BlockPos brewingStandPos;
  @Nullable private BrewingStandBlockEntity brewingStand;
  private int timeToRecalcPath;
  private int findCool;
  private int playSoundCool;

  public PharmcistMode(
      ModeType<? extends PharmcistMode> modeType, String name, LittleMaidEntity mob) {
    super(modeType, name);
    this.mob = mob;
  }

  @Override
  public boolean shouldExecute() {
    if (0 < --findCool) {
      return false;
    }
    findCool = 20;

    // モードが中断されたあと、再開するときの判定
    BrewingStandBlockEntity prev;
    if (brewingStandPos != null
        && brewingStandPos.isWithinDistance(this.mob.getPos(), 6)
        && (prev = getBrewingStandBlockEntity(brewingStandPos).orElse(null)) != null
        && !isUsingByOtherMaid(brewingStandPos)) {
      brewingStand = prev;
      // 醸造中、またはアイテムが残っている場合はtrue
      if (isBrewing(prev) || !isBrewingStandEmpty(prev)) {
        return true;
      }
    } else {
      brewingStandPos = null;
    }

    // 醸造を開始するときの判定

    // 材料がないならリターン
    if (getIngredient().isEmpty()) {
      return false;
    }
    // ポーション瓶がないならリターン
    if (getPotionBottle().isEmpty()) {
      return false;
    }

    // 醸造台が無いか、使えない場合は再探索
    if (brewingStandPos == null || brewingStand == null || !canBrewWith(brewingStand)) {
      brewingStandPos = findBrewingStandPos().orElse(null);
      if (brewingStandPos == null) {
        return false;
      }
      brewingStand = getBrewingStandBlockEntity(brewingStandPos).orElseThrow();
      return true;
    }
    return true;
  }

  /** インベントリから醸造材料のスロットを取得 */
  public OptionalInt getIngredient() {
    Inventory inventory = this.mob.getInventory();
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && BrewingRecipeRegistry.isValidIngredient(stack)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  /** インベントリからポーション瓶のスロットを取得 */
  public OptionalInt getPotionBottle() {
    Inventory inventory = this.mob.getInventory();
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && isPotionItem(stack)) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  /** ポーション瓶として醸造台に入れられるアイテムかどうか */
  private boolean isPotionItem(ItemStack stack) {
    return stack.isOf(Items.POTION)
        || stack.isOf(Items.SPLASH_POTION)
        || stack.isOf(Items.LINGERING_POTION)
        || stack.isOf(Items.GLASS_BOTTLE);
  }

  /** 使用可能な醸造台を探索する */
  public Optional<BlockPos> findBrewingStandPos() {
    return BlockFinder.searchTargetBlock(
        this.mob.getBlockPos(),
        this::isTargetBrewingStand,
        this::isSearchable,
        Arrays.asList(Direction.values()),
        128);
  }

  public boolean isTargetBrewingStand(BlockPos pos) {
    if (isUsingByOtherMaid(pos)) {
      return false;
    }
    return getBrewingStandBlockEntity(pos)
        .filter(this::isBrewingStandEmpty)
        .filter(this::canBrewWith)
        .isPresent();
  }

  public Optional<BrewingStandBlockEntity> getBrewingStandBlockEntity(BlockPos pos) {
    if (pos == null) {
      return Optional.empty();
    }
    BlockEntity tile = mob.getWorld().getBlockEntity(pos);
    if (tile instanceof BrewingStandBlockEntity brewingStandBlockEntity) {
      return Optional.of(brewingStandBlockEntity);
    }
    return Optional.empty();
  }

  private boolean isBrewingStandEmpty(BrewingStandBlockEntity stand) {
    for (int i = 0; i < stand.size(); i++) {
      if (!stand.getStack(i).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /** 手持ちのアイテムで醸造可能な醸造台かどうか */
  private boolean canBrewWith(@Nullable BrewingStandBlockEntity stand) {
    if (stand == null) {
      return false;
    }
    Inventory inventory = this.mob.getInventory();
    // 材料を探す
    ItemStack ingredient = ItemStack.EMPTY;
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && BrewingRecipeRegistry.isValidIngredient(stack)) {
        ingredient = stack;
        break;
      }
    }
    if (ingredient.isEmpty()) {
      return false;
    }
    // ポーション瓶と材料の組み合わせで醸造可能か確認
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty()
          && isPotionItem(stack)
          && BrewingRecipeRegistry.hasRecipe(stack, ingredient)) {
        return true;
      }
    }
    return false;
  }

  public boolean isUsingByOtherMaid(BlockPos pos) {
    var user = USED_BREWING_STAND_MAP.get(pos);
    if (user != null && user != this.mob) {
      if (!user.isAlive() || user != user.getWorld().getEntityById(user.getId())) {
        USED_BREWING_STAND_MAP.remove(pos);
        return false;
      }
      return true;
    }
    return false;
  }

  public boolean isSearchable(BlockPos pos) {
    BlockState state;
    return Math.abs(pos.getY() - this.mob.getY()) < 2
        && pos.isWithinDistance(this.mob.getPos(), 6)
        && ((state = this.mob.getWorld().getBlockState(pos))
                .canPathfindThrough(this.mob.getWorld(), pos, NavigationType.LAND)
            || (state.getBlock() instanceof DoorBlock
                && ((DoorBlock) state.getBlock()).getBlockSetType().canOpenByHand()));
  }

  private boolean isBrewing(BrewingStandBlockEntity stand) {
    return ((BrewingStandAccessor) stand).getBrewTime_LM() > 0;
  }

  @Override
  public void startExecuting() {
    findCool = 0;
    USED_BREWING_STAND_MAP.put(brewingStandPos, mob);
    mob.play(LMSounds.COOKING_START);
    playSoundCool = 20;
  }

  @Override
  public boolean shouldContinueExecuting() {
    if (brewingStandPos == null) {
      return false;
    }
    // 醸造台が変わっていたら終了
    var tmp = getBrewingStandBlockEntity(brewingStandPos).orElse(null);
    if (tmp != brewingStand) {
      brewingStandPos = null;
      brewingStand = null;
      return false;
    }

    // 醸造中ならtrue
    if (isBrewing(brewingStand)) {
      return true;
    }

    // ポーション瓶スロットにアイテムがあればtrue（取り出す必要がある）
    for (int i = POTION_SLOT_START; i <= POTION_SLOT_END; i++) {
      if (!brewingStand.getStack(i).isEmpty()) {
        return true;
      }
    }
    // 材料スロットにアイテムが残っていればtrue
    if (!brewingStand.getStack(INGREDIENT_SLOT).isEmpty()) {
      return true;
    }

    // まだ醸造可能な材料とポーション瓶があればtrue
    return getIngredient().isPresent() && getPotionBottle().isPresent();
  }

  @Override
  public void tick() {
    if (brewingStandPos == null || brewingStand == null) {
      return;
    }
    // SpotBugs対策: フィールドをローカル変数にキャッシュ
    BrewingStandBlockEntity stand = brewingStand;

    // 視線を向ける
    this.mob
        .getLookControl()
        .lookAt(
            brewingStandPos.getX() + 0.5,
            brewingStandPos.getY() + 0.5,
            brewingStandPos.getZ() + 0.5);

    // 醸造台の近くに移動
    if (!this.mob.getBlockPos().isWithinDistance(brewingStandPos, 1.75)) {
      if (this.mob.isSneaking()) {
        this.mob.setSneaking(false);
      }
      if (--this.timeToRecalcPath <= 0) {
        this.timeToRecalcPath = 10;
        double x = brewingStandPos.getX() + 0.5D;
        double y = brewingStandPos.getY() + 0.5D;
        double z = brewingStandPos.getZ() + 0.5D;
        var path = this.mob.getNavigation().findPathTo(x, y, z, 2);
        this.mob.getNavigation().startMovingAlong(path, 1);
      }
      return;
    }
    this.mob.getNavigation().stop();

    // しゃがむ
    if (!this.mob.isSneaking()) {
      this.mob.setSneaking(true);
    }

    playSoundCool--;

    // 醸造中は待機
    if (isBrewing(stand)) {
      return;
    }

    Inventory inventory = this.mob.getInventory();

    // 完成品を取り出す
    tryExtractPotions(stand, inventory);

    // 材料スロットに残ったガラス瓶を取り出す
    tryExtractIngredientSlot(stand, inventory);

    // 燃料が不足していればブレイズパウダーを挿入
    tryInsertFuel(stand, inventory);

    // ポーション瓶を挿入
    tryInsertPotionBottles(stand, inventory);

    // 材料を挿入
    getIngredient().ifPresent(idx -> tryInsertIngredient(stand, inventory, idx));
  }

  private void tryInsertFuel(BrewingStandBlockEntity stand, Inventory inventory) {
    // 燃料がまだあるなら不要
    if (((BrewingStandAccessor) stand).getFuel_LM() > 0) {
      return;
    }
    // 燃料スロットにすでにブレイズパウダーがあるなら不要
    if (!stand.getStack(FUEL_SLOT).isEmpty()) {
      return;
    }
    // インベントリからブレイズパウダーを探す
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack stack = inventory.getStack(i);
      if (!stack.isEmpty() && stack.isOf(Items.BLAZE_POWDER)) {
        ItemStack toInsert = stack.split(1);
        stand.setStack(FUEL_SLOT, toInsert);
        stand.markDirty();
        pickupAction();
        if (playSoundCool < 0) {
          playSoundCool = 20;
          mob.play(LMSounds.ADD_FUEL);
        }
        return;
      }
    }
  }

  private void tryInsertPotionBottles(BrewingStandBlockEntity stand, Inventory inventory) {
    // 材料を先に確認（材料とのレシピがあるポーション瓶のみ入れる）
    ItemStack ingredient = ItemStack.EMPTY;
    // 醸造台の材料スロットにすでにある場合はそれを使う
    ItemStack standIngredient = stand.getStack(INGREDIENT_SLOT);
    if (!standIngredient.isEmpty() && BrewingRecipeRegistry.isValidIngredient(standIngredient)) {
      ingredient = standIngredient;
    } else {
      // インベントリから材料を探す
      for (int i = 0; i < inventory.size(); ++i) {
        ItemStack stack = inventory.getStack(i);
        if (!stack.isEmpty() && BrewingRecipeRegistry.isValidIngredient(stack)) {
          ingredient = stack;
          break;
        }
      }
    }
    if (ingredient.isEmpty()) {
      return;
    }

    for (int slot = POTION_SLOT_START; slot <= POTION_SLOT_END; slot++) {
      if (!stand.getStack(slot).isEmpty()) {
        continue;
      }
      // インベントリからレシピが成立するポーション瓶を探して挿入
      for (int i = 0; i < inventory.size(); ++i) {
        ItemStack potionStack = inventory.getStack(i);
        if (potionStack.isEmpty() || !isPotionItem(potionStack)) {
          continue;
        }
        if (!BrewingRecipeRegistry.hasRecipe(potionStack, ingredient)) {
          continue;
        }
        // 1つだけ挿入
        ItemStack toInsert = potionStack.split(1);
        stand.setStack(slot, toInsert);
        stand.markDirty();
        pickupAction();
        break;
      }
    }
  }

  private void tryInsertIngredient(
      BrewingStandBlockEntity stand, Inventory inventory, int ingredientIndex) {
    // 材料スロットが空の場合のみ挿入
    if (!stand.getStack(INGREDIENT_SLOT).isEmpty()) {
      return;
    }
    // ポーション瓶スロットにアイテムがない場合は挿入しない
    boolean hasPotionInStand = false;
    for (int i = POTION_SLOT_START; i <= POTION_SLOT_END; i++) {
      if (!stand.getStack(i).isEmpty()) {
        hasPotionInStand = true;
        break;
      }
    }
    if (!hasPotionInStand) {
      return;
    }

    ItemStack ingredientStack = inventory.getStack(ingredientIndex);
    ItemStack toInsert = ingredientStack.split(1);
    stand.setStack(INGREDIENT_SLOT, toInsert);
    stand.markDirty();
    pickupAction();
    if (playSoundCool < 0) {
      playSoundCool = 20;
      mob.play(LMSounds.COOKING_START);
    }
  }

  private void tryExtractPotions(BrewingStandBlockEntity stand, Inventory inventory) {
    for (int slot = POTION_SLOT_START; slot <= POTION_SLOT_END; slot++) {
      ItemStack potionStack = stand.getStack(slot);
      if (potionStack.isEmpty()) {
        continue;
      }
      // まだ同じ材料で醸造できる場合は取り出さない
      ItemStack ingredient = stand.getStack(INGREDIENT_SLOT);
      if (!ingredient.isEmpty() && BrewingRecipeRegistry.hasRecipe(potionStack, ingredient)) {
        continue;
      }
      // インベントリにも材料がある場合は確認
      boolean canBrewMore = false;
      if (ingredient.isEmpty()) {
        for (int i = 0; i < inventory.size(); ++i) {
          ItemStack invStack = inventory.getStack(i);
          if (!invStack.isEmpty()
              && BrewingRecipeRegistry.isValidIngredient(invStack)
              && BrewingRecipeRegistry.hasRecipe(potionStack, invStack)) {
            canBrewMore = true;
            break;
          }
        }
      }
      if (canBrewMore) {
        continue;
      }

      // 取り出す
      if (tryTransferToInventory(stand, slot, inventory)) {
        if (playSoundCool < 0) {
          playSoundCool = 20;
          mob.play(LMSounds.COOKING_OVER);
        }
      }
    }
  }

  private void tryExtractIngredientSlot(BrewingStandBlockEntity stand, Inventory inventory) {
    ItemStack ingredientStack = stand.getStack(INGREDIENT_SLOT);
    if (ingredientStack.isEmpty()) {
      return;
    }
    // ガラス瓶（醸造後の残り）なら取り出す
    if (ingredientStack.isOf(Items.GLASS_BOTTLE)) {
      tryTransferToInventory(stand, INGREDIENT_SLOT, inventory);
    }
  }

  private boolean tryTransferToInventory(
      BrewingStandBlockEntity stand, int slot, Inventory inventory) {
    ItemStack stack = stand.getStack(slot);
    if (stack.isEmpty()) {
      return false;
    }
    // インベントリの空きスロットに入れる
    for (int i = 0; i < inventory.size(); ++i) {
      ItemStack invStack = inventory.getStack(i);
      if (invStack.isEmpty()) {
        inventory.setStack(i, stack.copy());
        stand.removeStack(slot);
        stand.markDirty();
        pickupAction();
        return true;
      }
      // 同じアイテムでスタック可能なら合流
      if (ItemStack.canCombine(invStack, stack) && invStack.getCount() < invStack.getMaxCount()) {
        int transferable = Math.min(stack.getCount(), invStack.getMaxCount() - invStack.getCount());
        invStack.increment(transferable);
        stack.decrement(transferable);
        if (stack.isEmpty()) {
          stand.removeStack(slot);
          stand.markDirty();
          pickupAction();
          return true;
        }
      }
    }
    return false;
  }

  public void pickupAction() {
    this.mob.swingHand(Hand.MAIN_HAND);
    this.mob.playSound(
        SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRandom().nextFloat() * 0.1F + 1.0F);
  }

  @Override
  public void resetTask() {
    playSoundCool = 0;
    this.mob.setSneaking(false);
    if (brewingStandPos != null) {
      USED_BREWING_STAND_MAP.remove(brewingStandPos, mob);
      BrewingStandBlockEntity stand = getBrewingStandBlockEntity(brewingStandPos).orElse(null);
      if (stand == null) {
        brewingStandPos = null;
        return;
      }
      // 醸造台からアイテムをすべて取り出す
      Inventory inventory = this.mob.getInventory();
      for (int i = 0; i < stand.size(); i++) {
        tryTransferToInventory(stand, i, inventory);
      }
    }
  }

  @Override
  public void writeModeData(NbtCompound nbt) {
    if (brewingStandPos != null) {
      nbt.put("BrewingStandPos", NbtHelper.fromBlockPos(brewingStandPos));
    }
  }

  @Override
  public void readModeData(NbtCompound nbt) {
    if (nbt.contains("BrewingStandPos")) {
      brewingStandPos = NbtHelper.toBlockPos(nbt.getCompound("BrewingStandPos"));
    }
  }
}
