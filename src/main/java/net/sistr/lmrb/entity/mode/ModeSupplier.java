package net.sistr.lmrb.entity.mode;


import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public interface ModeSupplier {

    Optional<Mode> getMode();

    void writeModeData(CompoundTag tag);

    void readModeData(CompoundTag tag);

}
