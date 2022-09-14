package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public interface Tameable {

    Optional<LivingEntity> getTameOwner();

    void setTameOwnerUuid(UUID id);

    Optional<UUID> getTameOwnerUuid();

    boolean hasTameOwner();

    boolean isWait();

    void setWait(boolean isWait);

}
