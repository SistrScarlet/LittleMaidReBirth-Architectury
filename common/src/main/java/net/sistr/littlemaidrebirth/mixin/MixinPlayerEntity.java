package net.sistr.littlemaidrebirth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.iff.*;
import net.sistr.littlemaidrebirth.util.PlayerAccessor;
import net.sistr.littlemaidrebirth.util.PlayerInventoryAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements PlayerAccessor, PlayerInventoryAccessor, HasIFF {
    private final HasIFF iff = new IFFImpl();

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    protected abstract void collideWithEntity(Entity entity);

    @Mutable
    @Shadow
    @Final
    private PlayerInventory inventory;

    @Override
    public void onCollideWithEntity_LM(Entity entity) {
        collideWithEntity(entity);
    }

    @Override
    public PlayerInventory getPlayerInventory_LMRB() {
        return this.inventory;
    }

    @Override
    public void setPlayerInventory_LMRB(PlayerInventory inventory) {
        this.inventory = inventory;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(World world, BlockPos blockPos, float f, GameProfile gameProfile, CallbackInfo ci) {
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
    public void updatePassengerPosition(Entity passenger) {
        if (!(passenger instanceof LittleMaidEntity)) {
            super.updatePassengerPosition(passenger);
        }
        if (!this.hasPassenger(passenger)) {
            return;
        }
        float z = -6 / 16f * 0.9375F;
        float y = (float) (this.getMountedHeightOffset() - 4 / 16f * 0.9375F + passenger.getHeightOffset());
        Vec3d pos = new Vec3d(z, 0.0, 0.0).rotateY((float) (-this.bodyYaw * (Math.PI / 180.0) - Math.PI / 2.0));
        passenger.setPosition(this.getX() + pos.x, this.getY() + (double) y, this.getZ() + pos.z);
        this.copyEntityData(passenger);
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        if (!(passenger instanceof LittleMaidEntity)) {
            super.onPassengerLookAround(passenger);
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
}
