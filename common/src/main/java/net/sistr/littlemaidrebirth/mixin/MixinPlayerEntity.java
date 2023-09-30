package net.sistr.littlemaidrebirth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.iff.*;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements HasIFF {
    private final HasIFF iff = new IFFImpl();

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
        this.setIFFs(IFFTypeManager.getINSTANCE().getIFFTypes(world).stream()
                .map(IFFType::createIFF).collect(Collectors.toList()));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    public void onRead(NbtCompound nbt, CallbackInfo ci) {
        this.readIFF(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    public void onWrite(NbtCompound nbt, CallbackInfo ci) {
        this.writeIFF(nbt);
    }

    @Override
    public Optional<IFFTag> identify(LivingEntity target) {
        return iff.identify(target);
    }

    @Override
    public void setIFFs(List<IFF> iffs) {
        iff.setIFFs(iffs);
    }

    @Override
    public List<IFF> getIFFs() {
        return iff.getIFFs();
    }

    @Override
    public void writeIFF(NbtCompound nbt) {
        iff.writeIFF(nbt);
    }

    @Override
    public void readIFF(NbtCompound nbt) {
        iff.readIFF(nbt);
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if (passenger instanceof LittleMaidEntity maid) {
            clampPassengerLMYaw(maid);
        }
    }

    @Unique
    protected void clampPassengerLMYaw(LittleMaidEntity maid) {
        maid.setBodyYaw(this.getBodyYaw());
        maid.setYaw(this.getBodyYaw());
        float maidHead2PlayerBody = MathHelper.wrapDegrees(this.getBodyYaw() - maid.getHeadYaw());
        float clamped = MathHelper.clamp(maidHead2PlayerBody, -maid.getMaxHeadRotation(), maid.getMaxHeadRotation());
        maid.setHeadYaw(maid.getHeadYaw() + maidHead2PlayerBody - clamped);
    }

    @Override
    protected Vector3f getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        if (passenger instanceof LittleMaidEntity) {
            float playerHeight = 1.8f;
            float percent = (playerHeight - 6f / 16f) / playerHeight;
            float z = -6 / 16f * 0.9375F;
            return new Vector3f(0.0f, dimensions.height * percent, z);
        }
        return super.getPassengerAttachmentPos(passenger, dimensions, scaleFactor);
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("RETURN"))
    private void onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {

    }
}
