package net.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmodelloader.util.Tuple;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;
import org.apache.commons.compress.utils.Lists;

import java.util.*;

/**
 * HasModeの移譲用クラス
 */
public class HasModeImpl implements HasMode {
    private final LivingEntity owner;
    private final HasInventory hasInventory;
    private final Set<Mode> modes = Sets.newHashSet();
    private final List<Tuple<ItemMatcher, Mode>> itemMatchers = new ObjectArrayList<>();
    private Mode nowMode;
    private Item prevItem = Items.AIR;
    private NbtCompound tempModeData;

    public HasModeImpl(LivingEntity owner, HasInventory hasInventory, Set<Mode> modes) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        this.modes.addAll(modes);
        updateMatchList();
    }

    protected void updateMatchList() {
        this.itemMatchers.clear();
        List<Tuple<Mode, Tuple<ItemMatcher.Priority, ItemMatcher>>> list = Lists.newArrayList();
        for (Mode mode : this.modes) {
            for (Tuple<ItemMatcher.Priority, ItemMatcher> tuple : mode.getModeType().getItemMatcherList()) {
                list.add(new Tuple<>(mode, tuple));
            }
        }
        this.modes.stream()
                .flatMap(mode -> mode.getModeType().getItemMatcherList().stream()
                        .map(tuple -> new Tuple<>(mode, tuple)))
                .sorted(Comparator.<Tuple<Mode, Tuple<ItemMatcher.Priority, ItemMatcher>>>
                                comparingInt(tuple -> tuple.getB().getA().get())
                        .reversed())
                .forEach(tuple -> this.itemMatchers.add(new Tuple<>(tuple.getB().getB(), tuple.getA())));
    }

    public void addMode(Mode mode) {
        modes.add(mode);
        updateMatchList();
    }

    public void addAllMode(Collection<Mode> mode) {
        modes.addAll(mode);
        updateMatchList();
    }

    @Override
    public Optional<Mode> getMode() {
        return Optional.ofNullable(this.nowMode);
    }

    @Override
    public void writeModeData(NbtCompound nbt) {
        getMode().ifPresent(mode -> {
            NbtCompound modeData = new NbtCompound();
            mode.writeModeData(modeData);
            nbt.put("ModeData", modeData);
        });
    }

    @Override
    public void readModeData(NbtCompound nbt) {
        if (nbt.contains("ModeData"))
            this.tempModeData = nbt.getCompound("ModeData");
    }

    public void tick() {
        if (!isModeContinue()) {
            //手持ちアイテムに現在のモードで適用できるかチェック
            if (hasModeItem()) {
                return;
            }
            changeMode();
        }
        prevItem = owner.getMainHandStack().getItem();
    }

    public boolean hasModeItem() {
        if (nowMode != null && owner.getMainHandStack().isEmpty()) {
            Inventory inventory = hasInventory.getInventory();
            for (int index = 0; index < inventory.size(); index++) {
                ItemStack stack = inventory.getStack(index);
                if (nowMode.getModeType().isModeItem(stack)) {
                    owner.setStackInHand(Hand.MAIN_HAND, stack.copy());
                    inventory.removeStack(index);
                    prevItem = owner.getMainHandStack().getItem();
                    return true;
                }
            }
        }
        return false;
    }

    //アイテムが同じ場合は継続
    public boolean isModeContinue() {
        return prevItem == owner.getMainHandStack().getItem();
    }

    public void changeMode() {
        Mode newMode = getNewMode();
        if (nowMode != newMode) {
            if (nowMode != null) {
                nowMode.resetTask();
                nowMode.endModeTask();
            }
            if (newMode != null) {
                if (tempModeData != null) {
                    newMode.readModeData(tempModeData);
                    tempModeData = null;
                }
                newMode.startModeTask();
            }
            nowMode = newMode;
        }
    }

    public Mode getNewMode() {
        var mainHand = owner.getMainHandStack();
        for (Tuple<ItemMatcher, Mode> tuple : this.itemMatchers) {
            if (tuple.getA().isMatch(mainHand)) {
                return tuple.getB();
            }
        }
        return null;
    }
}
