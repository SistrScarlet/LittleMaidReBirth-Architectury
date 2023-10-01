package net.sistr.littlemaidrebirth.entity;

import dev.architectury.networking.NetworkManager;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.world.WorldMaidSoulState;
import org.jetbrains.annotations.Nullable;

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
        float waveHeight = 1f;
        float x = (MathHelper.sin(rotate)) * range;
        float z = (MathHelper.cos(rotate)) * range;
        float y = MathHelper.sin(
                MathHelper.PI * 2
                        * ((float) (this.waveProgress % loop) / loop))
                * (waveHeight / 2);

        var particle = ParticleTypes.ELECTRIC_SPARK;

        float yOffset = 0.25f;
        var world = getWorld();
        world.addParticle(
                particle,
                this.getX() + x,
                this.getY() + y + yOffset,
                this.getZ() + z,
                0, 0, 0);
        world.addParticle(
                particle,
                this.getX() - x,
                this.getY() + y + yOffset,
                this.getZ() - z,
                0, 0, 0);
        world.addParticle(
                particle,
                this.getX() - x,
                this.getY() - y + yOffset,
                this.getZ() - z,
                0, 0, 0);
        world.addParticle(
                particle,
                this.getX() + x,
                this.getY() - y + yOffset,
                this.getZ() + z,
                0, 0, 0);

        this.waveProgress++;

        if (world instanceof ServerWorld serverWorld
                && maidSoul != null && maidSoul.getOwnerUUID().isPresent()) {
            var owner = serverWorld.getEntity(maidSoul.getOwnerUUID().get());
            if (owner != null) {
                var toOwnerVec = owner.getPos().subtract(this.getPos()).normalize();
                var distanceSq = Math.max(this.squaredDistanceTo(owner.getEyePos()), 0.5 * 0.5);
                var addVec = toOwnerVec.multiply(0.0125 / distanceSq);
                if (addVec.lengthSquared() > 0.001 * 0.001) {
                    setVelocity(getVelocity().add(addVec));
                }
            }
        }

        var velocity = getVelocity();
        double vx = Math.min(velocity.getX(), 0.2);
        double vy = Math.min(velocity.getY(), 0.2);
        double vz = Math.min(velocity.getZ(), 0.2);
        if (world.isSpaceEmpty(this, getBoundingBox().offset(vx, vy, vz))) {
            double nx = this.getX() + vx;
            double ny = this.getY() + vy;
            double nz = this.getZ() + vz;
            this.setPosition(nx, ny, nz);
            setVelocity(velocity.multiply(0.95f));
        } else {
            //進行方向が埋まっていて、逆方向が開いてるなら弾かれる
            if (world.isSpaceEmpty(this, getBoundingBox().offset(-vx, -vy, -vz))) {
                double nx = this.getX() - vx;
                double ny = this.getY() - vy;
                double nz = this.getZ() - vz;
                this.setPosition(nx, ny, nz);
                setVelocity(velocity.multiply(-0.95f));
            } else {
                setVelocity(Vec3d.ZERO);
            }

        }

        //埋まった場合はちょっとづつ浮く
        if (!world.isSpaceEmpty(this)) {
            this.setPosition(this.getX(), this.getY() + 0.1, this.getZ());
        }
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        super.onPlayerCollision(player);
        if (this.maidSoul == null) {
            this.discard();
            return;
        }
        Optional<UUID> optional;
        if ((optional = this.maidSoul.getOwnerUUID()).isPresent()
                && optional.get().equals(player.getUuid())) {
            player.sendPickup(this, 1);
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                var maidSoulState = WorldMaidSoulState.getWorldMaidSoulState(serverWorld);
                maidSoulState.add(player.getUuid(), this.maidSoul);
                maidSoulState.markDirty();
                serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS,
                        1.0f, 1.0f);
                float size = 0.5f;
                int count = 20;
                double delta = 1.0;
                //todo エフェクト調整
                serverWorld.spawnParticles(
                        new DustParticleEffect(new Vec3f(1.0f, 0.0f, 0.0f), size),
                        this.getX(), this.getY(), this.getZ(),
                        count, delta, delta, delta, 0);
                serverWorld.spawnParticles(
                        new DustParticleEffect(new Vec3f(0.0f, 1.0f, 0.0f), size),
                        this.getX(), this.getY(), this.getZ(),
                        count, delta, delta, delta, 0);
                serverWorld.spawnParticles(
                        new DustParticleEffect(new Vec3f(0.0f, 0.0f, 1.0f), size),
                        this.getX(), this.getY(), this.getZ(),
                        count, delta, delta, delta, 0);
                //todo 憑依ステータス効果
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

    @Override
    public Packet<?> createSpawnPacket() {
        return NetworkManager.createAddEntityPacket(this);
    }
}
