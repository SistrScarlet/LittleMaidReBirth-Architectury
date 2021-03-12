package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.entity.InventorySupplier;

//todo ちゃんと実装する
public class PharmcistMode<T extends PathAwareEntity & InventorySupplier> implements Mode {
    private final T mob;
    private final int inventoryStart;
    private final int inventoryEnd;

    public PharmcistMode(T mob, int inventoryStart, int inventoryEnd) {
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }


    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return false;
    }

    @Override
    public void startExecuting() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void resetTask() {

    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundTag tag) {

    }

    @Override
    public void readModeData(CompoundTag tag) {

    }

    @Override
    public String getName() {
        return "Pharmcist";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(Items.GLASS_BOTTLE);
        ModeManager.INSTANCE.register(PharmcistMode.class, items);
    }

}
