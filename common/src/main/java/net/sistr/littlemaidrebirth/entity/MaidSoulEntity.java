package net.sistr.littlemaidrebirth.entity;

import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.world.WorldMaidSoulState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

//メイドソウル
//体重21g！
public class MaidSoulEntity extends Entity {
    @Nullable
    private LittleMaidEntity.MaidSoul maidSoul;
    private int waveProgress;

    public MaidSoulEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    public MaidSoulEntity(World world, @Nullable LittleMaidEntity.MaidSoul maidSoul) {
        this(Registration.MAID_SOUL_ENTITY.get(), world);
        this.maidSoul = maidSoul;
    }

    @Override
    public void tick() {
        int loop = 20 * 4;
        //上端下端のときrange = 0
        //waveProgressが0/半分/最後のときが上端下端
        float range = MathHelper.sin(
                MathHelper.PI
                        * ((float) ((this.waveProgress + loop / 4) % (loop / 2)) / (loop / 2f)))
                * 0.4f + 0.1f;
        int rotateTicks = 20 * 1;
        float rotate = MathHelper.PI * 2 * ((float) (this.age % rotateTicks) / rotateTicks);
        float altRotate = -MathHelper.PI * 2 * ((float) ((this.age) % rotateTicks) / rotateTicks);
        float waveHeight = 1f;
        float x = (MathHelper.sin(rotate)) * range;
        float altX = (MathHelper.sin(MathHelper.PI * 2 - altRotate)) * range;
        float z = (MathHelper.cos(rotate)) * range;
        float altZ = (MathHelper.cos(MathHelper.PI * 2 - altRotate)) * range;
        float y = MathHelper.sin(
                MathHelper.PI * 2
                        * ((float) (this.waveProgress % loop) / loop))
                * (waveHeight / 2);

        getWorld().addParticle(
                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f),
                this.getX() + x,
                this.getY() + y,
                this.getZ() + z,
                0, 0, 0);
        getWorld().addParticle(
                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f),
                this.getX() - x,
                this.getY() + y,
                this.getZ() - z,
                0, 0, 0);
        /*getWorld().addParticle(
                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f),
                this.getX() + altX,
                this.getY() - y,
                this.getZ() + altZ,
                0, 0, 0);*/
        /*getWorld().addParticle(
                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f),
                this.getX() - altX,
                this.getY() - y,
                this.getZ() - altZ,
                0, 0, 0);*/
        getWorld().addParticle(
                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f),
                this.getX() - x,
                this.getY() - y,
                this.getZ() - z,
                0, 0, 0);
        getWorld().addParticle(
                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f),
                this.getX() + x,
                this.getY() - y,
                this.getZ() + z,
                0, 0, 0);

        this.waveProgress++;

        if (!this.getWorld().isSpaceEmpty(this)) {
            this.setPosition(this.getX(), this.getY() + 0.1, this.getZ());
        }
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        super.onPlayerCollision(player);
        Optional<UUID> optional;
        if (maidSoul != null
                && (optional = this.maidSoul.getOwnerUUID()).isPresent()
                && optional.get().equals(player.getUuid())) {
            player.sendPickup(this, 1);
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                var maidSoulState = WorldMaidSoulState.getWorldMaidSoulState(serverWorld);
                maidSoulState.add(player.getUuid(), this.maidSoul);
                maidSoulState.markDirty();
            }
            this.discard();
        }
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("maidSoul")) {
            this.maidSoul = new LittleMaidEntity.MaidSoul(nbt.getCompound("maidSoul"));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.maidSoul != null) {
            nbt.put("maidSoul", this.maidSoul.getNbt().copy());
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return false;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
    }

    @Override
    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }

    @Override
    public boolean canAvoidTraps() {
        return true;
    }

    public int getWaveProgress() {
        return waveProgress;
    }
}
