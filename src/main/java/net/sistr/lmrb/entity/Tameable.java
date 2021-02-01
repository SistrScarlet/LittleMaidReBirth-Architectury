package net.sistr.lmrb.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

public interface Tameable {

    Optional<LivingEntity> getTameOwner();

    void setTameOwnerUuid(UUID id);

    Optional<UUID> getTameOwnerUuid();

    boolean hasTameOwner();

    MovingState getMovingState();

    void setMovingState(MovingState movingState);

    void setFreedomPos(BlockPos pos);

    BlockPos getFreedomPos();

    enum MovingState {
        WAIT("Wait", 0),
        ESCORT("Escort", 1),
        FREEDOM("Freedom", 2);
        private final String name;
        private final int id;

        MovingState(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public static MovingState fromName(String name) {
            for (MovingState state : MovingState.values()) {
                if (state.getName().equals(name)) {
                    return state;
                }
            }
            throw new IllegalArgumentException("存在しないMovingStateです。 : " + name);
        }

        public static MovingState fromId(int id) {
            for (MovingState state : MovingState.values()) {
                if (state.getId() == id) {
                    return state;
                }
            }
            throw new IllegalArgumentException("存在しないMovingStateです。 : " + id);
        }
    }

}
