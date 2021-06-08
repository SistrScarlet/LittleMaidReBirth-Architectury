package net.sistr.littlemaidrebirth.entity.mode;


import net.minecraft.nbt.CompoundTag;
import net.sistr.littlemaidrebirth.api.mode.Mode;

import java.util.Optional;

public interface ModeSupplier {

    Optional<Mode> getMode();

    void writeModeData(CompoundTag tag);

    void readModeData(CompoundTag tag);

}
