package net.sistr.littlemaidrebirth.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManager;
import net.sistr.littlemaidrebirth.entity.util.MaidManagerImpl;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.world.WorldMaidSoulState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity implements MaidManager {
    @Shadow
    public abstract ServerWorld getServerWorld();

    @Unique
    private final MaidManager maidManager = new MaidManagerImpl();

    protected MixinServerPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void onCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        // ターゲットタグ
        var thisSync = this.getTargetTagsSync();
        var oldSync = ((TargetTagManager) oldPlayer).getTargetTagsSync();
        thisSync.syncFrom(oldSync);

        // メイドさん管理
        migrateWorldMaidSoulState();
        this.checkMaidUnload();
        var nbt = new NbtCompound();
        ((MaidManager) oldPlayer).writeMaidManager(oldPlayer.writeNbt(nbt));
        this.readMaidManager(nbt);
    }

    @Inject(method = "trySleep", at = @At("RETURN"))
    public void onTrySleep(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        if (this.isSleeping()) {
            getAroundTamedSoundPlayable()
                    .forEach(e -> e.play(LMSounds.GOOD_NIGHT));
        }
    }

    @Inject(method = "wakeUp", at = @At("RETURN"))
    public void onWakeUp(boolean bl, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (!bl && !updateSleepingPlayers) {
            getAroundTamedSoundPlayable()
                    .forEach(s -> s.play(LMSounds.GOOD_MORNING));
        }
    }

    private Stream<SoundPlayable> getAroundTamedSoundPlayable() {
        return this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(8),
                        e -> e instanceof Tameable tameable
                                && TameableUtil.getTameOwnerUuid(tameable)
                                .filter(id -> id.equals(this.getUuid()))
                                .isPresent() && e instanceof SoundPlayable
                ).stream()
                .map(e -> (SoundPlayable) e)
                .filter(s -> !(s instanceof LivingEntity)
                        || (((LivingEntity) s).getMainHandStack().getItem() == Items.CLOCK
                        || ((LivingEntity) s).getOffHandStack().getItem() == Items.CLOCK)
                );
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void onReadSP(NbtCompound nbt, CallbackInfo ci) {
        this.readMaidManager(nbt);
        migrateWorldMaidSoulState();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void onWriteSP(NbtCompound nbt, CallbackInfo ci) {
        this.checkMaidUnload();
        this.writeMaidManager(nbt);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.getRandom().nextInt(20) == 0) {
            this.checkMaidUnload();
        }
    }

    /**
     * メイドソウルをワールド管理からプレイヤー管理に移行する
     */
    @Unique
    private void migrateWorldMaidSoulState() {
        WorldMaidSoulState worldMaidSoulState = WorldMaidSoulState.getWorldMaidSoulState(getServerWorld());
        worldMaidSoulState.get(this.getUuid())
                .forEach(this.maidManager::registerMaid);
        worldMaidSoulState.remove(this.getUuid());
    }

    @Override
    public void registerMaid(MaidSoulEntity soul) {
        this.maidManager.registerMaid(soul);
    }

    @Override
    public void registerMaid(LittleMaidEntity maid) {
        this.maidManager.registerMaid(maid);
    }

    @Override
    public void registerMaid(LittleMaidEntity.MaidSoul soul) {
        this.maidManager.registerMaid(soul);
    }

    @Override
    public List<MaidManager.LMInfo> getMaidList() {
        return this.maidManager.getMaidList();
    }

    @Override
    public void writeMaidManager(NbtCompound nbt) {
        this.maidManager.writeMaidManager(nbt);
    }

    @Override
    public void readMaidManager(NbtCompound nbt) {
        this.maidManager.readMaidManager(nbt);
    }

    @Override
    public List<LittleMaidEntity.MaidSoul> getMaidSouls() {
        return this.maidManager.getMaidSouls();
    }

    @Override
    public void clearMaidSouls() {
        this.maidManager.clearMaidSouls();
    }

    @Override
    public void checkMaidUnload() {
        this.maidManager.checkMaidUnload();
    }
}
