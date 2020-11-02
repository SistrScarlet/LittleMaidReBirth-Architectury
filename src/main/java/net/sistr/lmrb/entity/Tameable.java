package net.sistr.lmrb.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

public interface Tameable {

    String NONE = "None";
    String WAIT = "Wait";
    String ESCORT = "Escort";
    String FREEDOM = "Freedom";

    Optional<LivingEntity> getTameOwner();

    void setTameOwnerUuid(UUID id);

    Optional<UUID> getTameOwnerUuid();

    boolean hasTameOwner();

    String getMovingState();

    void setMovingState(String movingState);

    Optional<BlockPos> getFollowPos();

}
