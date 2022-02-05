package net.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.Optional;

public interface HasIFF {

    Optional<IFFTag> identify(LivingEntity target);

    void setIFFs(List<IFF> iffs);

    List<IFF> getIFFs();

    void writeIFF(NbtCompound nbt);

    void readIFF(NbtCompound nbt);

}
