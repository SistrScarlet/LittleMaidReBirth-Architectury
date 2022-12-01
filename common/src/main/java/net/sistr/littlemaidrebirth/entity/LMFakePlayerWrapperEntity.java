package net.sistr.littlemaidrebirth.entity;

import com.mojang.datafixers.DataFixer;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.util.PlayerAdvancementTrackerWrapper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;

public abstract class LMFakePlayerWrapperEntity<T extends LittleMaidEntity> extends FakePlayerWrapperEntity<T> {

    public LMFakePlayerWrapperEntity(T origin) {
        super(origin);
    }

    @Override
    protected void pickupItems() {
        if (LMRBMod.getConfig().isCanPickupItemByNoOwner()
                || getOrigin().getTameOwnerUuid().isPresent()) {
            super.pickupItems();
        }
    }

    @Override
    public PlayerAdvancementTracker initFPWEAdvancementTracker(DataFixer dataFixer, PlayerManager playerManager, ServerAdvancementLoader serverAdvancementLoader, File file, ServerPlayerEntity serverPlayerEntity) {
        return new LMFakeAdvancementTracker<>(dataFixer, playerManager, serverAdvancementLoader, file, this);
    }

    public static class LMFakeAdvancementTracker<T extends LittleMaidEntity>
            extends PlayerAdvancementTracker
            implements PlayerAdvancementTrackerWrapper {
        private final LMFakePlayerWrapperEntity<T> fakePlayer;

        public LMFakeAdvancementTracker(DataFixer dataFixer, PlayerManager playerManager,
                                        ServerAdvancementLoader advancementLoader,
                                        File advancementFile, LMFakePlayerWrapperEntity<T> owner) {
            super(dataFixer, playerManager, advancementLoader, advancementFile, owner);
            this.fakePlayer = owner;
        }

        private Optional<PlayerAdvancementTracker> getAltTracker() {
            return fakePlayer.getOrigin().getTameOwner()
                    .filter(l -> l instanceof ServerPlayerEntity)
                    .map(l -> (ServerPlayerEntity) l)
                    .map(ServerPlayerEntity::getAdvancementTracker);
        }

        @Override
        public void setOwner(ServerPlayerEntity owner) {
        }

        @Override
        public void clearCriteria() {
        }

        @Override
        public void reload(ServerAdvancementLoader advancementLoader) {
        }

        @Override
        public void save() {
        }

        @Override
        public boolean grantCriterion(Advancement advancement, String criterionName) {
            return false;
        }

        @Override
        public boolean revokeCriterion(Advancement advancement, String criterionName) {
            return false;
        }

        @Override
        public void sendUpdate(ServerPlayerEntity player) {
        }

        @Override
        public void setDisplayTab(@Nullable Advancement advancement) {
        }

        @Override
        public AdvancementProgress getProgress(Advancement advancement) {
            return this.getAltTracker()
                    .map(a -> a.getProgress(advancement))
                    .orElse(super.getProgress(advancement));
        }

        @Override
        public boolean isNonFileAdvancement() {
            return true;
        }
    }

}
