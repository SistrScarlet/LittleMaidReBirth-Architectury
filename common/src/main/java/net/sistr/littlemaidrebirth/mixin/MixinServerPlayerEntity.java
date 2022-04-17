package net.sistr.littlemaidrebirth.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.entity.Tameable;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity {

    protected MixinServerPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void onCopy(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.setIFFs(((HasIFF) oldPlayer).getIFFs());
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
        return this.world.getOtherEntities(this, this.getBoundingBox().expand(8),
                        e -> e instanceof Tameable && ((Tameable) e).getTameOwnerUuid()
                                .filter(id -> id.equals(this.getUuid()))
                                .isPresent() && e instanceof SoundPlayable
                ).stream()
                .map(e -> (SoundPlayable) e)
                .filter(s -> !(s instanceof LivingEntity)
                        || (((LivingEntity) s).getMainHandStack().getItem() == Items.CLOCK
                        || ((LivingEntity) s).getOffHandStack().getItem() == Items.CLOCK)
                );
    }

}
