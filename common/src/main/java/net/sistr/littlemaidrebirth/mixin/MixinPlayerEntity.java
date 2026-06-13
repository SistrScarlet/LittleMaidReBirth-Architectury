package net.sistr.littlemaidrebirth.mixin;

import com.mojang.authlib.GameProfile;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.targeting.TargetIdentifier;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManager;
import net.sistr.littlemaidrebirth.entity.targeting.TargetTagManagerImpl;
import net.sistr.littlemaidrebirth.entity.targeting.TargetingSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements TargetTagManager {
  // todo TargetTagManagerはServerPlayer側で実装すべきかもしれない
  @Unique private TargetTagManager targetTagManager;

  protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
    super(entityType, world);
  }

  @Inject(method = "<init>", at = @At("RETURN"))
  public void onInit(
      World world, BlockPos pos, float yaw, GameProfile gameProfile, CallbackInfo ci) {
    this.targetTagManager = new TargetTagManagerImpl(world);
  }

  @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
  public void onRead(NbtCompound nbt, CallbackInfo ci) {
    this.readTargetTags(nbt);
  }

  @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
  public void onWrite(NbtCompound nbt, CallbackInfo ci) {
    this.writeTargetTags(nbt);
  }

  @Override
  public Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id) {
    return this.targetTagManager.getTargetTag(id);
  }

  @Override
  public void readTargetTags(NbtCompound nbt) {
    this.targetTagManager.readTargetTags(nbt);
  }

  @Override
  public void writeTargetTags(NbtCompound nbt) {
    this.targetTagManager.writeTargetTags(nbt);
  }

  @Override
  public Sync getTargetTagsSync() {
    return this.targetTagManager.getTargetTagsSync();
  }

  @Override
  public void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
    if (!(passenger instanceof LittleMaidEntity)) {
      super.updatePassengerPosition(passenger, positionUpdater);
      return;
    }
    if (!this.hasPassenger(passenger)) {
      return;
    }
    float z = -6 / 16f * 0.9375F;
    // 1.21 で getMountedHeightOffset / getHeightOffset が削除されたため
    // 1.20.1 のデフォルト実装（高さ * 0.75、passenger offset = 0）で近似
    float y = (float) (this.getHeight() * 0.75F - 4 / 16f * 0.9375F);
    Vec3d pos =
        new Vec3d(z, 0.0, 0.0).rotateY((float) (-this.bodyYaw * (Math.PI / 180.0) - Math.PI / 2.0));
    positionUpdater.accept(
        passenger, this.getX() + pos.x, this.getY() + (double) y, this.getZ() + pos.z);
    this.copyEntityData(passenger);
  }

  @Override
  public void onPassengerLookAround(Entity passenger) {
    if (!(passenger instanceof LittleMaidEntity)) {
      super.onPassengerLookAround(passenger);
      return;
    }
    copyEntityData(passenger);
  }

  protected void copyEntityData(Entity entity) {
    float yaw = this.bodyYaw;
    entity.setBodyYaw(yaw);
    float f = MathHelper.wrapDegrees(yaw - this.getYaw());
    float f1 = MathHelper.clamp(f, -105.0F, 105.0F);
    entity.prevYaw += f1 - f;
    entity.setYaw(yaw + f1 - f);
    entity.setHeadYaw(yaw);
  }

  @Inject(method = "wakeUp(ZZ)V", at = @At("RETURN"))
  private void onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {}
}
