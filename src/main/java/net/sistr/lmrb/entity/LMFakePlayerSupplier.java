package net.sistr.lmrb.entity;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class LMFakePlayerSupplier implements FakePlayerSupplier {
    private final LittleMaidEntity origin;
    private FakePlayer fakePlayer;

    public LMFakePlayerSupplier(LittleMaidEntity origin) {
        this.origin = origin;
    }

    @Override
    public FakePlayer getFakePlayer() {
        if (this.fakePlayer == null) {
            this.fakePlayer = new FakePlayerWrapperEntity(this.origin) {
                @Override
                public LivingEntity getOrigin() {
                    return origin;
                }

                @Override
                public Optional<PlayerAdvancementTracker> getOriginAdvancementTracker() {
                    return origin.getTameOwner()
                            .map(owner -> ((ServerPlayerEntity)owner))
                            .map(ServerPlayerEntity::getAdvancementTracker);
                }
            };
        }
        return this.fakePlayer;
    }

    void tick() {
        if (fakePlayer != null)
            fakePlayer.tick();
    }

}
