package net.sistr.littlemaidrebirth.entity.mode;


import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidrebirth.api.mode.Mode;

import java.util.Optional;

public interface ModeSupplier {

    Optional<Mode> getMode();

    void writeModeData(NbtCompound nbt);

    void readModeData(NbtCompound nbt);

}
