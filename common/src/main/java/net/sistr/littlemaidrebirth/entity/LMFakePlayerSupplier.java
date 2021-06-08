package net.sistr.littlemaidrebirth.entity;

import net.minecraft.advancement.PlayerAdvancementTracker;
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
            this.fakePlayer = new LMFakePlayerWrapperEntity<LittleMaidEntity>(this.origin) {

                @Override
                public LittleMaidEntity getOrigin() {
                    return origin;
                }

                @Override
                public Optional<PlayerAdvancementTracker> getOriginAdvancementTracker() {
                    return origin.getTameOwner()
                            .map(owner -> ((ServerPlayerEntity) owner))
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
