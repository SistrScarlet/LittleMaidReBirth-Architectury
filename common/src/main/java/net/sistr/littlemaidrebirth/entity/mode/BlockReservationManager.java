package net.sistr.littlemaidrebirth.entity.mode;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class BlockReservationManager {
    public static final BlockReservationManager INSTANCE = new BlockReservationManager();

    private final Map<GlobalPos, LittleMaidEntity> reservations = new HashMap<>();

    public boolean isReservedByOther(World world, BlockPos pos, LittleMaidEntity maid) {
        GlobalPos globalPos = GlobalPos.create(world.getRegistryKey(), pos.toImmutable());
        LittleMaidEntity existing = reservations.get(globalPos);
        if (existing != null && existing != maid) {
            if (isValid(existing)) {
                return true;
            }
            reservations.remove(globalPos);
        }
        return false;
    }

    public void reserve(World world, BlockPos pos, LittleMaidEntity maid) {
        GlobalPos globalPos = GlobalPos.create(world.getRegistryKey(), pos.toImmutable());
        reservations.put(globalPos, maid);
    }

    public void release(World world, BlockPos pos, LittleMaidEntity maid) {
        GlobalPos globalPos = GlobalPos.create(world.getRegistryKey(), pos.toImmutable());
        reservations.remove(globalPos, maid);
    }

    private boolean isValid(LittleMaidEntity maid) {
        return maid.isAlive() && maid == maid.getWorld().getEntityById(maid.getId());
    }
}
