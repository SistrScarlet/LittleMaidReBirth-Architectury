package net.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidmodelloader.util.Tuple;
import net.sistr.littlemaidrebirth.api.mode.ItemMatcher;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.entity.util.HasInventory;

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

    public HasModeImpl(LivingEntity owner, HasInventory hasInventory, Set<Mode> modes) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        this.modes.addAll(modes);
        updateMatchList();
    }

    protected void updateMatchList() {
        this.itemMatchers.clear();
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
        if (this.nowMode != null) {
            ModeManager.INSTANCE.getId(nowMode)
                    .ifPresent(identifier -> {
                        nbt.putString("ModeID", identifier.toString());
                        NbtCompound modeData = new NbtCompound();
                        nowMode.writeModeData(modeData);
                        nbt.put("ModeData", modeData);
                    });
        }
    }

    @Override
    public void readModeData(NbtCompound nbt) {
        if (nbt.contains("ModeType") && nbt.contains("ModeData")) {
            var modeData = nbt.getCompound("ModeData");
            var modeID = Identifier.tryParse(nbt.getString("ModeID"));
            if (modeID != null) {
                // modesに一致するものがあればピック
                ModeManager.INSTANCE.getType(modeID)
                        .flatMap(modeType -> modes.stream()
                                .filter(mode -> mode.getModeType() == modeType)
                                .findFirst())
                        .ifPresent(mode -> {
                            mode.readModeData(modeData);
                            nowMode = mode;
                        });
            }
        }
    }

    public void tick() {
        // モード無しなら新たな
        if (nowMode == null) {
            getNewMode().ifPresent(this::changeNewMode);
            return;
        }
        if (!isModeContinue()) {
            // 手持ちアイテムに現在のモードで適用できるかチェック
            var index = getNowModeItemIndex();
            if (index == -1) {
                // モード続行不可なら新たなモードに切り替える
                getNewMode().ifPresent(this::changeNewMode);
            } else {
                // モードアイテムがあるならメインハンドと入れ替え
                switchMainHandItem(index);
            }
        }
    }

    // 現在のモードのモードアイテムがインベントリにあるならTrue
    public int getNowModeItemIndex() {
        if (nowMode == null) return -1;

        var inv = hasInventory.getInventory();
        for (int index = 0; index < inv.size(); index++) {
            var stack = inv.getStack(index);
            if (nowMode.getModeType().isModeItem(stack)) {
                return index;
            }
        }
        return -1;
    }

    // メインハンドとインベントリのアイテムを入れ替える
    public void switchMainHandItem(int index) {
        var inv = hasInventory.getInventory();
        ItemStack invStack = inv.getStack(index);
        var tmp = owner.getMainHandStack();

        owner.setStackInHand(Hand.MAIN_HAND, invStack);
        inv.setStack(index, tmp);
    }

    // モードを継続するか
    // 所持アイテムが現在のモードを有効にするならTrue
    public boolean isModeContinue() {
        if (nowMode == null) return false;
        var stack = owner.getMainHandStack();
        return nowMode.getModeType().isModeItem(stack);
    }

    // モードを切り替える
    public void changeNewMode(Mode mode) {
        if (nowMode != null) {
            nowMode.resetTask();
            nowMode.endModeTask();
        }
        mode.startModeTask();
        nowMode = mode;
    }

    // 現在メインハンドにあるアイテムが有効にするモードを返す
    public Optional<Mode> getNewMode() {
        var mainHand = owner.getMainHandStack();
        if (mainHand.isEmpty()) {
            return Optional.empty();
        }
        for (Tuple<ItemMatcher, Mode> tuple : this.itemMatchers) {
            if (tuple.getA().isMatch(mainHand)) {
                return Optional.of(tuple.getB());
            }
        }
        return Optional.empty();
    }
}
