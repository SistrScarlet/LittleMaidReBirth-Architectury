package net.sistr.lmrb.entity.iff;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public interface HasIFF {

    IFFTag identify(LivingEntity target);

    void setIFFs(List<IFF> iffs);

    List<IFF> getIFFs();

    void writeIFF(CompoundTag tag);

    void readIFF(CompoundTag tag);

}
